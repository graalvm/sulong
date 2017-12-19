/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.runtime;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.runtime.datalayout.DataLayoutConverter.DataSpecConverterImpl;
import com.oracle.truffle.llvm.runtime.debug.LLVMSourceContext;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.memory.LLVMThreadingStack;
import com.oracle.truffle.llvm.runtime.options.SulongEngineOption;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.DataSpecConverter;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;

import sun.misc.Unsafe;

public final class LLVMContext {

    private final List<Path> libraryPaths = new ArrayList<>();
    private final List<ExternalLibrary> externalLibraries = new ArrayList<>();

    private DataSpecConverterImpl targetDataLayout;

    private final List<RootCallTarget> globalVarInits = new ArrayList<>();
    private final List<RootCallTarget> globalVarDeallocs = new ArrayList<>();
    private final List<RootCallTarget> constructorFunctions = new ArrayList<>();
    private final List<RootCallTarget> destructorFunctions = new ArrayList<>();
    private final Deque<LLVMFunctionDescriptor> atExitFunctions = new ArrayDeque<>();
    private final List<LLVMThread> runningThreads = new ArrayList<>();
    private final LLVMThreadingStack threadingStack;
    private final Object[] mainArguments;
    private final Map<String, String> environment;
    private Source mainSourceFile;
    private boolean bcLibrariesLoaded;
    private final LinkedList<LLVMAddress> caughtExceptionStack = new LinkedList<>();
    private final LinkedList<DestructorStackElement> destructorStack = new LinkedList<>();
    private final HashMap<String, Integer> nativeCallStatistics;
    private final Object handlesLock;
    private final IdentityHashMap<TruffleObject, LLVMAddress> toNative;
    private final HashMap<LLVMAddress, TruffleObject> toManaged;
    private final LLVMSourceContext sourceContext;
    private final LLVMGlobalsStack globalStack;

    private final Env env;
    private final LLVMScope globalScope;
    private final LLVMFunctionPointerRegistry functionPointerRegistry;

    private final List<ContextExtension> contextExtension;

    private final ThreadLocal<LLVMAddress> tls = ThreadLocal.withInitial(LLVMAddress::nullPointer);
    private final ThreadLocal<LLVMAddress> clearChildTid = ThreadLocal.withInitial(LLVMAddress::nullPointer);

    private final MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(new Object[0]);
    private final FrameDescriptor globalFrameDescriptor = globalFrame.getFrameDescriptor();

    // #define SIG_DFL ((__sighandler_t) 0) /* Default action. */
    private final LLVMAddress sigDfl;

    // # define SIG_IGN ((__sighandler_t) 1) /* Ignore signal. */
    private final LLVMAddress sigIgn;

    // #define SIG_ERR ((__sighandler_t) -1) /* Error return. */
    private final LLVMAddress sigErr;

    private long nextBlockID = 0;
    private final Map<String, Long> uniqueBlockIDs = new HashMap<>();
    private final Map<Long, FrameSnapshot> setjmpEnvironments = new HashMap<>();

    public static final class FrameSnapshot {
        private final FrameDescriptor descriptor;
        private final Object[] values;

        private FrameSnapshot(FrameDescriptor descriptor, Object[] values) {
            this.descriptor = descriptor;
            this.values = values;
        }

        public FrameDescriptor getFrameDescriptor() {
            return descriptor;
        }

        public Object[] getValues() {
            return values;
        }
    }

    public static final class LLVMGlobalsStack {

        static final Unsafe UNSAFE = getUnsafe();

        private static Unsafe getUnsafe() {
            CompilerAsserts.neverPartOfCompilation();
            try {
                Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
                singleoneInstanceField.setAccessible(true);
                return (Unsafe) singleoneInstanceField.get(null);
            } catch (Exception e) {
                throw new AssertionError();
            }
        }

        private final long lowerBounds;
        private final long upperBounds;

        private static final int ALIGNMENT = 8;
        private static final int SIZE = 81920;

        private long stackPointer;

        public LLVMGlobalsStack() {
            long stackAllocation = UNSAFE.allocateMemory(SIZE * 1024);
            this.lowerBounds = stackAllocation;
            this.upperBounds = stackAllocation + SIZE * 1024;
            this.stackPointer = upperBounds;
        }

        @TruffleBoundary
        public void free() {
            UNSAFE.freeMemory(lowerBounds);
        }

        public long allocateStackMemory(final long size) {
            assert size >= 0;
            final long alignedAllocation = (stackPointer - size) & -ALIGNMENT;
            assert alignedAllocation <= stackPointer;
            stackPointer = alignedAllocation;
            return alignedAllocation;
        }

    }

    public static final class DestructorStackElement {
        private final LLVMFunctionDescriptor destructor;
        private final long thiz;

        public DestructorStackElement(LLVMFunctionDescriptor destructor, LLVMAddress thiz) {
            this.destructor = destructor;
            this.thiz = thiz.getVal();
        }

        public LLVMFunctionDescriptor getDestructor() {
            return destructor;
        }

        public LLVMAddress getThiz() {
            return LLVMAddress.fromLong(thiz);
        }
    }

    private static final class LLVMFunctionPointerRegistry {
        private int currentFunctionIndex = 0;
        private final HashMap<LLVMAddress, LLVMFunctionDescriptor> functionDescriptors = new HashMap<>();

        synchronized LLVMFunctionDescriptor getDescriptor(LLVMAddress pointer) {
            return functionDescriptors.get(pointer);
        }

        synchronized void register(LLVMAddress pointer, LLVMFunctionDescriptor desc) {
            functionDescriptors.put(pointer, desc);
        }

        synchronized LLVMFunctionDescriptor create(FunctionFactory factory) {
            LLVMFunctionDescriptor fn = factory.create(currentFunctionIndex++);
            if (fn.isNullFunction()) {
                assert !functionDescriptors.containsKey(LLVMAddress.nullPointer());
                functionDescriptors.put(LLVMAddress.nullPointer(), fn);
            }
            return fn;
        }
    }

    public LLVMContext(Env env, List<ContextExtension> contextExtension) {
        this.env = env;
        this.contextExtension = contextExtension;

        this.globalStack = new LLVMGlobalsStack();
        this.nativeCallStatistics = SulongEngineOption.isTrue(env.getOptions().get(SulongEngineOption.NATIVE_CALL_STATS)) ? new HashMap<>() : null;
        this.threadingStack = new LLVMThreadingStack(Thread.currentThread(), env.getOptions().get(SulongEngineOption.STACK_SIZE_KB));
        this.sigDfl = LLVMAddress.fromLong(0);
        this.sigIgn = LLVMAddress.fromLong(1);
        this.sigErr = LLVMAddress.fromLong(-1);
        this.toNative = new IdentityHashMap<>();
        this.toManaged = new HashMap<>();
        this.handlesLock = new Object();
        this.functionPointerRegistry = new LLVMFunctionPointerRegistry();
        this.globalScope = LLVMScope.createGlobalScope(this);
        this.sourceContext = new LLVMSourceContext();

        Object mainArgs = env.getConfig().get(LLVMLanguage.MAIN_ARGS_KEY);
        this.mainArguments = mainArgs == null ? env.getApplicationArguments() : (Object[]) mainArgs;
        this.environment = System.getenv();

        addLibraryPaths(SulongEngineOption.getPolyglotOptionSearchPaths(env));
        addDefaultExternalLibraries();
        List<String> external = SulongEngineOption.getPolyglotOptionExternalLibraries(env);
        addExternalLibraries(external);
    }

    private void addDefaultExternalLibraries() {
        if (SulongEngineOption.isTrue(env.getOptions().get(SulongEngineOption.USE_LIBC_BITCODE))) {
            ExternalLibrary libc = addExternalLibrary("libc.bc");
            addExternalLibrary("sulong-libc-extensions.bc", libc);
        } else {
            addExternalLibrary("libsulong.bc");
        }
    }

    public long getBlockID(String name) {
        synchronized (uniqueBlockIDs) {
            Long id = uniqueBlockIDs.get(name);
            if (id == null) {
                id = nextBlockID++;
                uniqueBlockIDs.put(name, id);
            }
            assert id < nextBlockID; // check for wrap-around
            return id;
        }
    }

    public void storeSetjmpEnvironment(long id, FrameDescriptor descriptor, Object[] values) {
        setjmpEnvironments.put(id, new FrameSnapshot(descriptor, values));
    }

    public FrameSnapshot getSetjmpEnvironment(long id) {
        return setjmpEnvironments.get(id);
    }

    public LLVMGlobalsStack getGlobalsStack() {
        return globalStack;
    }

    public void addExternalLibraries(List<String> external) {
        for (String l : external) {
            addExternalLibrary(l);
        }
    }

    public <T> T getContextExtension(Class<T> type) {
        CompilerAsserts.neverPartOfCompilation();
        for (ContextExtension ce : contextExtension) {
            if (ce.extensionClass() == type) {
                return type.cast(ce);
            }
        }
        throw new IllegalStateException("No context extension for: " + type);
    }

    public boolean hasContextExtension(Class<?> type) {
        CompilerAsserts.neverPartOfCompilation();
        for (ContextExtension ce : contextExtension) {
            if (ce.extensionClass() == type) {
                return true;
            }
        }
        return false;
    }

    public int getByteAlignment(Type type) {
        return type.getAlignment(targetDataLayout);
    }

    public int getByteSize(Type type) {
        return type.getSize(targetDataLayout);
    }

    public int getBytePadding(long offset, Type type) {
        return Type.getPadding(offset, type, targetDataLayout);
    }

    public long getIndexOffset(long index, AggregateType type) {
        return type.getOffsetOf(index, targetDataLayout);
    }

    public DataSpecConverter getDataSpecConverter() {
        return targetDataLayout;
    }

    public ExternalLibrary addExternalLibrary(String lib, ExternalLibrary libraryToReplace) {
        CompilerAsserts.neverPartOfCompilation();
        Path path = locateExternalLibrary(lib);
        ExternalLibrary externalLib = new ExternalLibrary(path, libraryToReplace);
        int index = externalLibraries.indexOf(externalLib);
        if (index < 0) {
            externalLibraries.add(externalLib);
            return externalLib;
        } else {
            return externalLibraries.get(index);
        }
    }

    public ExternalLibrary addExternalLibrary(String lib) {
        return addExternalLibrary(lib, null);
    }

    public List<ExternalLibrary> getExternalLibraries(Predicate<ExternalLibrary> filter) {
        return externalLibraries.stream().filter(f -> filter.test(f)).collect(Collectors.toList());
    }

    public void addLibraryPaths(List<String> paths) {
        for (String p : paths) {
            addLibraryPath(p);
        }
    }

    private void addLibraryPath(String p) {
        Path path = Paths.get(p);
        if (path.toFile().exists()) {
            if (!libraryPaths.contains(path)) {
                libraryPaths.add(path);
            }
        }
    }

    @TruffleBoundary
    private Path locateExternalLibrary(String lib) {
        Path libPath = Paths.get(lib);
        if (libPath.isAbsolute()) {
            if (libPath.toFile().exists()) {
                return libPath;
            } else {
                throw new LinkageError(String.format("Library \"%s\" does not exist.", lib));
            }
        }

        for (Path p : libraryPaths) {
            Path absPath = Paths.get(p.toString(), lib);
            if (absPath.toFile().exists()) {
                return absPath;
            }
        }

        return libPath;
    }

    public Env getEnv() {
        return env;
    }

    public LLVMScope getGlobalScope() {
        return globalScope;
    }

    public ThreadLocal<LLVMAddress> getThreadLocalStorage() {
        return tls;
    }

    public ThreadLocal<LLVMAddress> getClearChildTid() {
        return clearChildTid;
    }

    @TruffleBoundary
    public LLVMFunctionDescriptor getFunctionDescriptor(LLVMAddress handle) {
        return functionPointerRegistry.getDescriptor(handle);
    }

    @TruffleBoundary
    public LLVMFunctionDescriptor createFunctionDescriptor(FunctionFactory factory) {
        return functionPointerRegistry.create(factory);
    }

    @TruffleBoundary
    public void registerFunctionPointer(LLVMAddress address, LLVMFunctionDescriptor descriptor) {
        functionPointerRegistry.register(address, descriptor);
    }

    public LLVMAddress getSigDfl() {
        return sigDfl;
    }

    public LLVMAddress getSigIgn() {
        return sigIgn;
    }

    public LLVMAddress getSigErr() {
        return sigErr;
    }

    @TruffleBoundary
    public TruffleObject getManagedObjectForHandle(LLVMAddress address) {
        synchronized (handlesLock) {
            final TruffleObject object = toManaged.get(address);

            if (object == null) {
                throw new UnsupportedOperationException("Cannot resolve native handle: " + address);
            }

            return object;
        }
    }

    @TruffleBoundary
    public void releaseHandle(LLVMMemory memory, LLVMAddress address) {
        synchronized (handlesLock) {
            final TruffleObject object = toManaged.get(address);

            if (object == null) {
                throw new UnsupportedOperationException("Cannot resolve native handle: " + address);
            }

            toManaged.remove(address);
            toNative.remove(object);
            memory.free(address);
        }
    }

    @TruffleBoundary
    public LLVMAddress getHandleForManagedObject(LLVMMemory memory, TruffleObject object) {
        synchronized (handlesLock) {
            return toNative.computeIfAbsent(object, (k) -> {
                LLVMAddress allocatedMemory = memory.allocateMemory(Long.BYTES);
                memory.putI64(allocatedMemory, 0xdeadbeef);
                toManaged.put(allocatedMemory, object);
                return allocatedMemory;
            });
        }
    }

    @TruffleBoundary
    public void registerNativeCall(LLVMFunctionDescriptor descriptor) {
        if (nativeCallStatistics != null) {
            String name = descriptor.getName() + " " + descriptor.getType();
            if (nativeCallStatistics.containsKey(name)) {
                int count = nativeCallStatistics.get(name) + 1;
                nativeCallStatistics.put(name, count);
            } else {
                nativeCallStatistics.put(name, 1);
            }
        }
    }

    public void printNativeCallStatistic() {
        if (nativeCallStatistics != null) {
            LinkedHashMap<String, Integer> sorted = nativeCallStatistics.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new));
            for (String s : sorted.keySet()) {
                System.err.println(String.format("Function %s \t count: %d", s, sorted.get(s)));
            }
        }
    }

    public LinkedList<LLVMAddress> getCaughtExceptionStack() {
        return caughtExceptionStack;
    }

    public LinkedList<DestructorStackElement> getDestructorStack() {
        return destructorStack;
    }

    public LLVMThreadingStack getThreadingStack() {
        return threadingStack;
    }

    public Object[] getMainArguments() {
        return mainArguments;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setMainSourceFile(Source mainSourceFile) {
        this.mainSourceFile = mainSourceFile;
    }

    public Source getMainSourceFile() {
        return mainSourceFile;
    }

    public void registerGlobalVarDealloc(RootCallTarget globalVarDealloc) {
        globalVarDeallocs.add(globalVarDealloc);
    }

    public void registerConstructorFunction(RootCallTarget constructorFunction) {
        constructorFunctions.add(constructorFunction);
    }

    public void registerDestructorFunction(RootCallTarget destructorFunction) {
        destructorFunctions.add(destructorFunction);
    }

    public void registerAtExitFunction(LLVMFunctionDescriptor atExitFunction) {
        atExitFunctions.push(atExitFunction);
    }

    public void registerGlobalVarInit(RootCallTarget globalVarInit) {
        globalVarInits.add(globalVarInit);
    }

    public synchronized void registerThread(LLVMThread thread) {
        assert !runningThreads.contains(thread);
        runningThreads.add(thread);
    }

    public synchronized void unregisterThread(LLVMThread thread) {
        runningThreads.remove(thread);
        assert !runningThreads.contains(thread);
    }

    @TruffleBoundary
    public synchronized void shutdownThreads() {
        // we need to iterate over a copy of the list, because stop() can modify the original list
        for (LLVMThread node : new ArrayList<>(runningThreads)) {
            node.stop();
        }
    }

    @TruffleBoundary
    public synchronized void awaitThreadTermination() {
        shutdownThreads();

        while (!runningThreads.isEmpty()) {
            LLVMThread node = runningThreads.get(0);
            node.awaitFinish();
            assert !runningThreads.contains(node); // should be unregistered by LLVMThreadNode
        }
    }

    public List<RootCallTarget> getGlobalVarDeallocs() {
        return globalVarDeallocs;
    }

    public List<RootCallTarget> getConstructorFunctions() {
        return constructorFunctions;
    }

    public List<RootCallTarget> getDestructorFunctions() {
        return destructorFunctions;
    }

    public Deque<LLVMFunctionDescriptor> getAtExitFunctions() {
        return atExitFunctions;
    }

    public List<RootCallTarget> getGlobalVarInits() {
        return globalVarInits;
    }

    public synchronized List<LLVMThread> getRunningThreads() {
        return Collections.unmodifiableList(runningThreads);
    }

    public boolean bcLibrariesLoaded() {
        return bcLibrariesLoaded;
    }

    public void setBcLibrariesLoaded() {
        bcLibrariesLoaded = true;
    }

    public interface FunctionFactory {
        LLVMFunctionDescriptor create(int index);
    }

    public void setDataLayoutConverter(DataSpecConverterImpl layout) {
        this.targetDataLayout = layout;
    }

    public LLVMSourceContext getSourceContext() {
        return sourceContext;
    }

    public MaterializedFrame getGlobalFrame() {
        return globalFrame;
    }

    public FrameSlot getGlobalFrameSlot(Object symbol, Type type) {
        FrameSlotKind kind;
        if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case DOUBLE:
                    kind = FrameSlotKind.Double;
                    break;
                case FLOAT:
                    kind = FrameSlotKind.Float;
                    break;
                case HALF:
                case I16:
                case I32:
                    kind = FrameSlotKind.Int;
                    break;
                case I1:
                    kind = FrameSlotKind.Boolean;
                    break;
                case I64:
                    kind = FrameSlotKind.Long;
                    break;
                case I8:
                    kind = FrameSlotKind.Byte;
                    break;
                default:
                    kind = FrameSlotKind.Object;
                    break;
            }
        } else {
            kind = FrameSlotKind.Object;
        }
        FrameSlot frameSlot = globalFrameDescriptor.findOrAddFrameSlot(symbol, type, kind);
        return frameSlot;
    }

    public static class ExternalLibrary {
        private final String name;
        private final Path path;
        private final ExternalLibrary libraryToReplace;

        public ExternalLibrary(Path path, ExternalLibrary libraryToReplace) {
            this.name = extractName(path);
            this.path = path;
            this.libraryToReplace = libraryToReplace;
        }

        public ExternalLibrary(String name) {
            this.name = name;
            this.path = null;
            this.libraryToReplace = null;
        }

        public Path getPath() {
            return path;
        }

        public ExternalLibrary getLibraryToReplace() {
            return libraryToReplace;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof ExternalLibrary) {
                ExternalLibrary other = (ExternalLibrary) obj;
                return name.equals(other.name) && Objects.equals(path, other.path);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ Objects.hashCode(path);
        }

        private static String extractName(Path path) {
            String nameWithExt = path.getFileName().toString();
            int lengthWithoutExt = nameWithExt.lastIndexOf(".");
            if (lengthWithoutExt > 0) {
                return nameWithExt.substring(0, lengthWithoutExt);
            }
            return nameWithExt;
        }
    }
}
