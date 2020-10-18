package com.praveen.javaagent;


import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.utility.JavaModule;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class AgentMain {
	public static void premain(String agentOps, Instrumentation inst) {
		System.out.println("Agent premain");

		new AgentBuilder.Default()
            .disableClassFormatChanges()
            // BEGIN -- low key didn't read exactly what this does exactly https://stackoverflow.com/questions/35968530/retransform-classes-with-byte-buddy
            // stack overflow thinks i need this inorder for my advice to work with hotspot jvm for transforming classes
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
            // END

            // gets me some debug logs
			// .with(AgentBuilder.Listener.StreamWriting.toSystemOut())

            // filter for the classes we actually want to look at
//			.type(any())
//			.transform(new AgentBuilder.Transformer() {
//				@Override
//				public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
//					try {
//						ClassInjector
//								.UsingInstrumentation
//								.of(Files.createTempDirectory("javaagent-temp-jars").toFile(), ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, inst)
//								.inject(Collections.singletonMap(TypeDescription.ForLoadedType.of(DebugTree.class), ClassFileLocator.ForClassLoader.read(DebugTree.class)));
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//					return builder;
//				}
//			})
			.type(named("play.api.mvc.Action"))
            .transform(
                new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                        // probably better to compose new AgentBuilder.Transformer.ForAdvice https://github.com/raphw/byte-buddy/blob/master/byte-buddy-dep/src/main/java/net/bytebuddy/asm/Advice.java#L149-L153
                        System.out.println("TypeDescription: " + typeDescription);
						System.out.println("classLoader: " + classLoader);
						System.out.println("JavaModule: " + module);


						try {
							ClassInjector
								.UsingInstrumentation
								.of(Files.createTempDirectory("javaagent-temp-jars").toFile(), ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, inst)
								.inject(Collections.singletonMap(TypeDescription.ForLoadedType.of(DebugTree.class), ClassFileLocator.ForClassLoader.read(DebugTree.class)));

						} catch (Exception e) {
							e.printStackTrace();
						}

						return builder
                                .visit(
                                    Advice
                                        .to(LoggingAdvice.class)
                                        .on(named("apply"))
                                );
                    }
                }
            )
            .type(not(isInterface()).and(isSubTypeOf(Runnable.class)))
            .transform(
                    new AgentBuilder.Transformer() {
                        @Override
                        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
//                            return builder
//                                    .visit(
//                                        Advice
//                                            .to(LoggingAdvice.class)
//                                            .on(isMethod())
//                                    );
								return builder;
                        }
                    }
            )
            .installOn(inst);
	}

	public static void agentmain(String agentOps, Instrumentation inst) {
		System.out.println("Agent main");
	}
}

class LoggingAdvice {

	@Advice.OnMethodEnter
	public static void onEnter(@Advice.Origin String origin, @Advice.AllArguments Object[] args) {
		System.out.println("STARTED -- " + origin);
		DebugTree.addMethodCall("bleh");
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void onExit(@Advice.Origin String origin) {
		System.out.println("ENDED -- " + origin);
		System.out.println("ENDED DEBUG TREE -- " + DebugTree.getTree());
	}
}


// new plan after doug killed my dreams of using InheritableThreadLocal
// if Action create DebugTreeRoot in ThreadLocal
// Forall things that implements runnable and callable check if there is a value in threadlocal with my debug class and if so copy over? some ish like that

