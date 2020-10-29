package com.praveen.javaagent;


import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;

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
//			 .with(AgentBuilder.Listener.StreamWriting.toSystemOut())

			// start debug tree
			.type(named("play.api.mvc.Action"))
            .transform(
                new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
						try {
							ClassInjector
								.UsingInstrumentation
								.of(Files.createTempDirectory("javaagent-temp-jars").toFile(), ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, inst)
								.inject(Collections.singletonMap(TypeDescription.ForLoadedType.of(DebugTree.class), ClassFileLocator.ForClassLoader.read(DebugTree.class)));

						} catch (Exception e) {
							e.printStackTrace();
						}

						// probably better to compose new AgentBuilder.Transformer.ForAdvice https://github.com/raphw/byte-buddy/blob/master/byte-buddy-dep/src/main/java/net/bytebuddy/asm/Advice.java#L149-L153
						return builder.visit(Advice.to(ActionAdvice.class).on(named("apply")));
                    }
                }
            )

//			// intercept runnable and callable constructor so that every time a runnable is created, create a field on the Runnable to store the debug tree
//            .type(not(isInterface().or(isAbstract())).and(isSubTypeOf(Runnable.class)))
//			.transform(new AgentBuilder.Transformer() {
//				@Override
//				public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
//					return builder
//							.defineField("debugTree", ArrayList.class)
//							.constructor(any())
//							.intercept(Advice.to(RunnableConstructorAdvice.class));
//				}
//			})
//
//			// get the debugTree stored in the field from last step and add it to current thread's ThreadLocal
//			.type(not(isInterface().or(isAbstract())).and(isSubTypeOf(Runnable.class)))
//			.transform(
//				new AgentBuilder.Transformer() {
//					@Override
//					public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
//						try {
//							ClassInjector
//									.UsingInstrumentation
//									.of(Files.createTempDirectory("javaagent-temp-jars").toFile(), ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, inst)
//									.inject(Collections.singletonMap(TypeDescription.ForLoadedType.of(DebugTree.class), ClassFileLocator.ForClassLoader.read(DebugTree.class)));
//
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//
//						return builder.visit(Advice.to(RunnableAdvice.class).on(named("run")));
//					}
//				}
//            )
            .type(not(isInterface().or(isAbstract())).and(isSubTypeOf(Runnable.class)))
            .transform(
                new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                        System.out.println("X " + typeDescription);
                        return builder.visit(Advice.to(RunnableConstructorAdvice.class).on(any()));
                    }
                }
            )
            .installOn(inst);
	}

	public static void agentmain(String agentOps, Instrumentation inst) {
		System.out.println("Agent main");
	}
}

//class DebugAdvice {
//
//	@Advice.OnMethodEnter
//	public static void onEnter(@Advice.Origin String origin, @Advice.AllArguments Object[] args) {
//		System.out.println("STARTED -- " + origin + " in thread " + Thread.currentThread().getName());
//		System.out.println("HelloService DebugService: " + DebugTree.getTree());
//	}
//
//	@Advice.OnMethodExit(onThrowable = Throwable.class)
//	public static void onExit(@Advice.Origin String origin) {
//		System.out.println("ENDED -- " + origin + " in thread " + Thread.currentThread().getName());
//		System.out.println("HelloService DebugService: " + DebugTree.getTree());
//	}
//}

class ActionAdvice {

	@Advice.OnMethodEnter
	public static void onEnter(@Advice.Origin String origin, @Advice.AllArguments Object[] args) {
		System.out.println("STARTED -- " + origin + " in thread " + Thread.currentThread().getName());
		DebugTree.addMethodCall("bleh");
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void onExit(@Advice.Origin String origin) {
		System.out.println("ENDED -- " + origin + " in thread " + Thread.currentThread().getName());
		System.out.println("ENDED DEBUG TREE -- " + DebugTree.getTree());
	}
}

class RunnableConstructorAdvice {

	@Advice.OnMethodEnter
	public static void onEnter(@Advice.Origin String origin) {
		System.out.println("Runnable Constructor onEnter: " + origin);
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void onExit(@Advice.Origin String origin) {
        System.out.println("Runnable Constructor onExit: " + origin);
	}
}

//class RunnableAdvice {
//
//	@Advice.OnMethodEnter
//	public static void onEnter(
//		@Advice.Origin String origin,
//		@Advice.AllArguments Object[] args,
//		@Advice.FieldValue(value = "debugTree", readOnly = false) ArrayList<String> debugTree
//	) {
//		System.out.println("Runnable getDebugTree: " + debugTree);
//	}
//
//	@Advice.OnMethodExit(onThrowable = Throwable.class)
//	public static void onExit(@Advice.Origin String origin) {
////		System.out.println(Thread.currentThread().getName() + " Runnable onExit DebugTree: " + DebugTree.state.toString());
//	}
//}

// new plan after doug killed my dreams of using InheritableThreadLocal
// if Action create DebugTreeRoot in ThreadLocal
// Forall things that implements runnable and callable check if there is a value in threadlocal with my debug class and if so copy over? some ish like that



// you have to make a runnable, and then schedule it
// when a runnable or callable is created we put in code into the constructor to say save the current thread's debug tree
// then onEnter of the run function we copy the value that we saved somewhere in the Runnable into the actual ThreadLocal of the thread actually executing the runnable

