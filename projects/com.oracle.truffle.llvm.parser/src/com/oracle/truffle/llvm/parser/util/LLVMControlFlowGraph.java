/*
 * Copyright (c) 2018, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.parser.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.llvm.parser.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.TerminatingInstruction;

public final class LLVMControlFlowGraph {

    private static final int LOOP_CONTAINER_MAX_CAPACITY = Long.SIZE;
    private static final int LOOP_HEADER_INITIAL_CAPACITY = 4;

    private CFGBlock[] blocks;
    private CFGLoop[] cfgLoops = new CFGLoop[LOOP_HEADER_INITIAL_CAPACITY];

    private int nextLoop = 0;

    private boolean reducible = true;

    public final class CFGBlock {

        private final InstructionBlock instructionBlock;

        public int id;
        public List<CFGBlock> sucs = new ArrayList<>();
        public List<CFGBlock> preds = new ArrayList<>();

        public boolean visited = false;
        public boolean active = false;
        public boolean isLoopHeader = false;
        public long loops;  // TODO could use bitmap instead
        public int loopId;

        public CFGBlock(InstructionBlock block) {
            this.instructionBlock = block;
            this.id = block.getBlockIndex();
        }

        @Override
        public String toString() {
            return instructionBlock.toString();
        }

    }

    public final class CFGLoop {
        private CFGBlock loopHeader;
        private List<CFGBlock> body;
        private List<CFGLoop> innerLoops;

        private Set<CFGBlock> successors;

        private final int id;

        public CFGLoop(int id) {
            this.id = id;
            body = new ArrayList<>();
            innerLoops = new ArrayList<>();
        }

        public List<CFGBlock> getBody() {
            return body;    // TODO maybe copy
        }

        public Set<CFGBlock> getSuccessors() {
            if (successors == null) {
                calculateSuccessors();
            }

            return this.successors;
        }

        /**
         * Calculates the successors of this loop with successors of inner loops potentially forwarded to
         * the outer loop
         */
        private void calculateSuccessors() {
            successors = new HashSet<>();

            for (CFGBlock s : loopHeader.sucs) {
                if (!isInLoop(s)) {
                    successors.add(s);
                }
            }

            for (CFGBlock b : body) {
                // for each inner loop, add all successors which are not in the outer loop
                // to the successors of the outer loop
                // TODO simplify
                if (b.isLoopHeader) {
                    for (CFGLoop l : innerLoops) {
                        if (l.getHeader().equals(b)) {
                            for (CFGBlock ib : l.getSuccessors()) {
                                if (!isInLoop(ib)) {
                                    successors.add(ib);
                                }
                            }
                        }
                    }
                } else {
                    for (CFGBlock s : b.sucs) {
                        if (!isInLoop(s)) {
                            successors.add(s);
                        }
                    }
                }
            }

        }

        public Integer[] getSuccessorIDs() {
            if (successors == null) {
                calculateSuccessors();
            }

            Integer[] sucIDs = new Integer[successors.size()];
            int i = 0;
            for (CFGBlock b : successors) {
                sucIDs[i++] = b.id;
            }

            return sucIDs;
        }

        public boolean isInLoop(CFGBlock block) {
            return block == loopHeader || body.contains(block);
        }

        public CFGBlock getHeader() {
            return loopHeader;
        }

        @Override
        public String toString() {
            return "Loop: " + this.id + " - Header: " + this.loopHeader.toString();
        }

    }

    public LLVMControlFlowGraph(InstructionBlock[] blocks) {
        this.blocks = new CFGBlock[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            this.blocks[i] = new CFGBlock(blocks[i]);
        }
    }

    private void resolveEdges() {
        for (CFGBlock block : blocks) {
            TerminatingInstruction term = block.instructionBlock.getTerminatingInstruction();
            // set successors and predecessors
            for (int i = 0; i < term.getSuccessorCount(); i++) {
                int sucId = term.getSuccessor(i).getBlockIndex();
                block.sucs.add(this.blocks[sucId]);
                this.blocks[sucId].preds.add(block);
            }
        }

    }

    public List<CFGLoop> getCFGLoops() {
        List<CFGLoop> loops = new ArrayList<>();

        for (CFGLoop l : cfgLoops) {
            if (l != null)
                loops.add(l);
        }
        loops.removeIf(l -> l == null);
        return loops;
    }

    public boolean isReducible() {
        return reducible;
    }

    public void build() {
// System.out.println("Start: " + System.currentTimeMillis());
// set successors and predecessors
        resolveEdges();
        long openLoops = openLoops(blocks[0]);
        if (openLoops != 0) {
            reducible = false;
            throw new RuntimeException("Irreducible control flow!"); // TODO bailout -> use dispatch approach
        }
        sortLoops();
// System.out.println("Successors: " + System.currentTimeMillis());
        for (CFGLoop l : getCFGLoops()) {
            l.calculateSuccessors();
        }
// System.out.println("End: " + System.currentTimeMillis());
    }

    private boolean sortLoops() {
        List<CFGLoop> sorted = new ArrayList<>();
        List<CFGLoop> active = new ArrayList<>();

        for (CFGLoop l : getCFGLoops()) {
            sortLoop(sorted, active, l);
        }

        cfgLoops = sorted.toArray(new CFGLoop[cfgLoops.length]);
        return true;
    }

    private void sortLoop(List<CFGLoop> sorted, List<CFGLoop> active, CFGLoop loop) {
        if (sorted.contains(loop))
            return;

        active.add(loop);
        for (CFGBlock b : loop.body) {
            if (b.isLoopHeader) {
                CFGLoop inner = cfgLoops[b.loopId];
                if (active.contains(inner)) {
                    // catches case that there is a stack overflow because two loop nodes are being called iteratively
                    // from one another, without one being left beforehand
                    throw new RuntimeException("Irreducible nestedness!");
                }
                sortLoop(sorted, active, inner);
                loop.innerLoops.add(inner);
            }
        }

        loop.body.sort(new Comparator<CFGBlock>() {

            @Override
            public int compare(CFGBlock o1, CFGBlock o2) {
                return o2.id - o1.id;
            }

        });

        sorted.add(loop);
        active.remove(loop);
    }

    private long openLoops(CFGBlock block) {
        if (block.visited) {
            if (block.active) {
                // Reached block via backward branch.
                makeLoopHeader(block);
                // Return cached loop information for this block.
                return block.loops;
            } else if (block.isLoopHeader) {
                return block.loops & ~(1L << block.loopId);
            } else {
                return block.loops;
            }
        }

        block.visited = true;
        block.active = true;

        long loops = 0;
        for (CFGBlock successor : block.sucs) {
            // Recursively process successors.
            loops |= openLoops(successor);
            if (successor.active) {
                // Reached block via backward branch.
                loops |= (1L << successor.loopId);  // TODO why?
            }
        }

        block.loops = loops;

        if (block.isLoopHeader) {
            loops &= ~(1L << block.loopId);
        }

        block.active = false;
        int inLoop = 0;

        // add blocks to all loops they are contained in
        long loopsToProcess = loops;
        while (loopsToProcess > 0) {
            inLoop = 64 - Long.numberOfLeadingZeros(loopsToProcess);
            loopsToProcess &= ~(1 << inLoop - 1);
            this.cfgLoops[inLoop - 1].body.add(block);
        }

        return loops;
    }

    private void makeLoopHeader(CFGBlock block) {
        if (!block.isLoopHeader) {
            block.isLoopHeader = true;

            if (nextLoop >= LOOP_CONTAINER_MAX_CAPACITY) {
                throw new RuntimeException("Too many loops!"); // TODO bailout
            }

            assert block.loops == 0;
            block.loops = 1L << nextLoop;

            if (nextLoop >= cfgLoops.length) {
                cfgLoops = Arrays.copyOf(cfgLoops, LOOP_CONTAINER_MAX_CAPACITY);
            }
            cfgLoops[nextLoop] = new CFGLoop(nextLoop);
            cfgLoops[nextLoop].loopHeader = block;
            block.loopId = nextLoop++;
        }
        assert Long.bitCount(block.loops) == 1;
    }
}
