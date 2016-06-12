suite = {
  "mxversion" : "5.19.4",
  "name" : "sulong",

  "imports" : {
    "suites" : [
        {
           "name" : "graal-core",
           "version" : "61191367671d802efa088b4b2c80f17bc3c53061",
           "urls" : [
                {"url" : "https://github.com/graalvm/graal-core", "kind" : "git"},
                {"url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind" : "binary"},
            ]
        },
    ],
  },

  "javac.lint.overrides" : "none",

  "libraries" : {
    "LLVM_IR_PARSER" : {
      "path" : "lib/com.intel.llvm.ireditor-1.0.8.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/com.intel.llvm.ireditor-1.0.8.jar",
      ],
      "sha1" : "7d8495fa6bbf3acb4faf4cedf42460be541e69dc",
      "maven" : {
        "groupId" : "parser",
        "artifactId" : "parser",
        "version" : "1.0.8",
      }
    },
    "EMF_COMMON" : {
      "path" : "lib/org.eclipse.emf.common_2.11.0.v20150512-0501.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/emf/org.eclipse.emf.common_2.11.0.v20150512-0501.jar",
      ],
      "sha1" : "2ee408923125830711b2817095010bce18ee8bb7",
      "maven" : {
        "groupId" : "emf",
        "artifactId" : "emfcommon",
        "version" : "2.11.0.v20150512-0501",
      }
    },
    "ECORE" : {
      "path" : "lib/org.eclipse.emf.ecore_2.11.0.v20150512-0501.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/emf/org.eclipse.emf.ecore_2.11.0.v20150512-0501.jar",
      ],
      "sha1" : "4dc95540c73cce54846ad976fbbe997a7f11aa9b",
      "maven" : {
        "groupId" : "emf",
        "artifactId" : "emf",
        "version" : "2.11.0.v20150512-0501.jar",
      }
    },
    "INJECT" : {
      "path" : "lib/com.google.inject_3.0.0.v201312141243.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/xtext/com.google.inject_3.0.0.v201312141243.jar",
      ],
      "sha1" : "2f5301dcdccf1a88b0022b932b6363825918d9a1",
      "maven" : {
        "groupId" : "xtext",
        "artifactId" : "inject",
        "version" : "2.11.0.v20150512-0501.jar",
      }
    },
    "XTEXT" : {
      "path" : "lib/org.eclipse.xtext_2.9.2.v201603040440.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/xtext/org.eclipse.xtext_2.9.2.v201603040440.jar",
      ],
      "sha1" : "17d747e9d13de0d07ca9139c8d457f8d251bccfc",
      "maven" : {
        "groupId" : "xtext",
        "artifactId" : "xtext",
        "version" : "2.8.4.v201508050135.jar",
      }
    },
    "XTEXT_XBASE" : {
      "path" : "lib/org.eclipse.xtext.xbase_2.9.2.v201603040440.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/xtext/org.eclipse.xtext.xbase_2.9.2.v201603040440.jar",
      ],
      "sha1" : "dac32b3a2021127f42eedc099d77a66109587dde",
      "maven" : {
        "groupId" : "xtext",
        "artifactId" : "xtext",
        "version" : "2.9.2.v201603040440.jar",
      }
    },
    "XTEXT_XBASE_LIB" : {
      "path" : "lib/org.eclipse.xtext.xbase.lib_2.9.2.v201603040440.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/xtext/org.eclipse.xtext.xbase.lib_2.9.2.v201603040440.jar",
      ],
      "sha1" : "7d184d42dd41a6c470007998a060fc07bad0a1b4",
      "maven" : {
        "groupId" : "xtext",
        "artifactId" : "xtext",
        "version" : "2.9.2.v201603040440.jar",
      }
    },
    "EMF_ECORE_XMI" : {
      "path" : "lib/org.eclipse.emf.ecore.xmi_2.11.0.v20150512-0501.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/emf/org.eclipse.emf.ecore.xmi_2.11.0.v20150512-0501.jar",
      ],
      "sha1" : "14711c456be51f16102bfd94cc5ad144b5dad4a3",
      "maven" : {
        "groupId" : "emf",
        "artifactId" : "emf",
        "version" : "2.11.0.v20150512-0501.jar",
      }
    },
    "XTEXT_TYPES" : {
      "path" : "lib/org.eclipse.xtext.common.types_2.8.4.v201508050135.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/xtext/org.eclipse.xtext.common.types_2.8.4.v201508050135.jar",
      ],
      "sha1" : "61dfb0e684ecf3a89392d151c440bafd99ff4711",
      "maven" : {
        "groupId" : "xtext",
        "artifactId" : "text",
        "version" : "2.8.4.v201508050135.jar",
      }
    },
    "XTEXT_JAVAX_INJECT" : {
      "path" : "lib/javax.inject_1.0.0.v20091030.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/xtext/javax.inject_1.0.0.v20091030.jar",
      ],
      "sha1" : "38623235627d561c3eb9a558de9a5535a1c30e29",
      "maven" : {
        "groupId" : "xtext",
        "artifactId" : "text",
        "version" : "javax.inject_1.0.0.v20091030.jar",
      }
    },
    "XTEXT_LOG4J" : {
      "path" : "lib/org.apache.log4j_1.2.15.v201012070815.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/xtext/org.apache.log4j_1.2.15.v201012070815.jar",
      ],
      "sha1" : "c8ec3aac571c457e84a039722a6b471a107c25bf",
      "maven" : {
        "groupId" : "xtext",
        "artifactId" : "text",
        "version" : "1.2.15.v201012070815.jar",
      }
    },
    "XTEXT_GOOGLE_GUAVA" : {
      "path" : "lib/com.google.guava_15.0.0.v201403281430.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/xtext/com.google.guava_15.0.0.v201403281430.jar",
      ],
      "sha1" : "6bc5d67ff18f033093fb493c0127a4219b1613a3",
      "maven" : {
        "groupId" : "xtext",
        "artifactId" : "text",
        "version" : "15.0.0.v201403281430.jar",
      }
    },
    "XTEXT_ANTLR_RUNTIME" : {
      "path" : "lib/org.antlr.runtime_3.2.0.v201101311130.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/xtext/org.antlr.runtime_3.2.0.v201101311130.jar",
      ],
      "sha1" : "94105115603f6e3276da3be15fc8d3186ed9e92e",
      "maven" : {
        "groupId" : "xtext",
        "artifactId" : "text",
        "version" : "3.2.0.v201101311130.jar",
      }
    },
    "XTEXT_UTIL" : {
      "path" : "lib/org.eclipse.xtext.util_2.8.4.v201508050135.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/xtext/org.eclipse.xtext.util_2.8.4.v201508050135.jar",
      ],
      "sha1" : "70616b797177f2e2b1b844f02a188e2837d648cb",
      "maven" : {
        "groupId" : "xtext",
        "artifactId" : "text",
        "version" : "2.8.4.v201508050135.jar",
      }
    },
    "ECLIPSE_EQUINOX" : {
      "path" : "lib/org.eclipse.equinox.common_3.6.200.v20130402-1505.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/sulong-deps/org.eclipse.equinox.common_3.6.200.v20130402-1505.jar",
      ],
      "sha1" : "550778d95ea4d5f2fee765e85eb799cec21067e0",
      "maven" : {
        "groupId" : "eclipse",
        "artifactId" : "eclipse",
        "version" : "3.6.200.v20130402-1505.jar",
      }
    },
    "LLVM_BITCODE_FOR_JAVA" : {
      "path" : "lib/llvm-bcj-1.0-SNAPSHOT.jar",
      "urls" : [
        "https://github.com/beehive-lab/llvm-bcj/releases/download/1.0-SNAPSHOT/llvm-bcj-1.0-SNAPSHOT.jar",
      ],
      "sha1" : "b7366a5c109595f307c4c358afcc0a5e8a70442f",
      "maven" : {
        "groupId" : "uk.ac.man.cs",
        "artifactId" : "llvm-bcj",
        "version" : "1.0-SNAPSHOT.jar",
      }
    },
  },

  "projects" : {

    "com.oracle.truffle.llvm.test" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm",
        "com.oracle.truffle.llvm.tools",
        "mx:JUNIT",
      ],
      "checkstyle" : "com.oracle.truffle.llvm.test",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.bench" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm",
      ],
      "checkstyle" : "com.oracle.truffle.llvm",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.tools" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm",
      ],
      "checkstyle" : "com.oracle.truffle.llvm.nodes",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.types" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm.runtime",
        "truffle:TRUFFLE_API",
      ],
      "checkstyle" : "com.oracle.truffle.llvm.nodes",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.types.test" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm.types",
        "mx:JUNIT",
      ],
      "checkstyle" : "com.oracle.truffle.llvm.test",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.runtime" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "truffle:TRUFFLE_API",
      ],
      "checkstyle" : "com.oracle.truffle.llvm.nodes",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.nodes" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "truffle:TRUFFLE_API",
      ],
      "checkstyle" : "com.oracle.truffle.llvm.nodes",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.nodes.impl" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm.nativeint",
      ],
      "checkstyle" : "com.oracle.truffle.llvm.nodes",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.intel.llvm.ireditor" : {
      "subDir" : "projects",
      "sourceDirs" : ["dummy-src"],
      "dependencies" : [
          "EMF_COMMON", "ECORE", "INJECT", "XTEXT", "XTEXT_XBASE", "XTEXT_XBASE_LIB", "EMF_ECORE_XMI", "XTEXT_TYPES", "XTEXT_JAVAX_INJECT", "XTEXT_LOG4J", "XTEXT_GOOGLE_GUAVA", "XTEXT_ANTLR_RUNTIME", "XTEXT_UTIL", "ECLIPSE_EQUINOX"
      ],
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.parser" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm.nodes",
        "com.oracle.truffle.llvm.types",
        "LLVM_IR_PARSER",
        "EMF_COMMON", "ECORE", "INJECT", "XTEXT", "XTEXT_XBASE", "XTEXT_XBASE_LIB", "EMF_ECORE_XMI", "XTEXT_TYPES", "XTEXT_JAVAX_INJECT", "XTEXT_LOG4J", "XTEXT_GOOGLE_GUAVA", "XTEXT_ANTLR_RUNTIME", "XTEXT_UTIL", "ECLIPSE_EQUINOX"
       ],
      "checkstyle" : "com.oracle.truffle.llvm",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.parser.impl" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm.nativeint"
      ],
      "checkstyle" : "com.oracle.truffle.llvm",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm.parser.factories",
        "com.oracle.truffle.llvm.parser.impl",
       ],
      "checkstyle" : "com.oracle.truffle.llvm",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.parser.factories" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm.nodes.impl",
       ],
      "checkstyle" : "com.oracle.truffle.llvm",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.nativeint" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "graal-core:GRAAL_TRUFFLE_HOTSPOT",
        "com.oracle.truffle.llvm.parser"
       ],
      "checkstyle" : "com.oracle.truffle.llvm",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },

    "com.oracle.truffle.llvm.parser.bc.impl" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.llvm.parser.factories",
        "LLVM_BITCODE_FOR_JAVA",
       ],
      "checkstyle" : "com.oracle.truffle.llvm",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle, LLVM",
      "license" : "BSD-new",
    },
  },

  "distributions" : {
    "SULONG" : {
      "path" : "build/sulong.jar",
      "subDir" : "graal",
      "sourcesPath" : "build/sulong.src.zip",
      "mainClass" : "com.oracle.truffle.llvm.LLVM",
      "dependencies" : ["com.oracle.truffle.llvm"],
      "exclude" : [
       "EMF_COMMON",
       "LLVM_IR_PARSER",
       "ECORE",
       "INJECT",
       "XTEXT",
       "XTEXT_XBASE",
       "XTEXT_XBASE_LIB",
       "EMF_ECORE_XMI",
       "XTEXT_TYPES",
       "XTEXT_JAVAX_INJECT",
       "XTEXT_LOG4J",
       "XTEXT_GOOGLE_GUAVA",
       "XTEXT_ANTLR_RUNTIME",
       "XTEXT_UTIL",
       "ECLIPSE_EQUINOX",
       "LLVM_BITCODE_FOR_JAVA",
      ],
      "distDependencies" : [
        "truffle:TRUFFLE_API",
        "graal-core:GRAAL_API",
        "graal-core:GRAAL_COMPILER",
        "graal-core:GRAAL_HOTSPOT",
        "graal-core:GRAAL_TRUFFLE_HOTSPOT",
      ]
  },
    "SULONG_TEST" : {
      "path" : "build/sulong_test.jar",
      "subDir" : "graal",
      "sourcesPath" : "build/sulong_test.src.zip",
      "dependencies" : ["com.oracle.truffle.llvm.test"],
      "exclude" : [
       "EMF_COMMON",
       "LLVM_IR_PARSER",
       "ECORE",
       "INJECT",
       "XTEXT",
       "XTEXT_XBASE",
       "XTEXT_XBASE_LIB",
       "EMF_ECORE_XMI",
       "XTEXT_TYPES",
       "XTEXT_JAVAX_INJECT",
       "XTEXT_LOG4J",
       "XTEXT_GOOGLE_GUAVA",
       "XTEXT_ANTLR_RUNTIME",
       "XTEXT_UTIL",
       "ECLIPSE_EQUINOX",
       "mx:JUNIT"
      ],
      "distDependencies" : [
        "truffle:TRUFFLE_API",
        "graal-core:GRAAL_API",
        "graal-core:GRAAL_COMPILER",
        "graal-core:GRAAL_HOTSPOT",
        "graal-core:GRAAL_TRUFFLE_HOTSPOT",
        "sulong:SULONG"
      ]
  },
 }
}
