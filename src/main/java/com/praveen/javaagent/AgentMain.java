package com.praveen.javaagent;

import java.lang.instrument.Instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;

public class AgentMain {
	public static void premain(String agentOps, Instrumentation inst) {
		System.out.println("Agent premain");
		new AgentBuilder.Default()
				.disableClassFormatChanges()
//				.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
				.with(AgentBuilder.Listener.StreamWriting.toSystemOut())
				.type(ElementMatchers.nameStartsWith("com.praveen.testapp")) // This handles just grabbing the classes we care about, will probably be com.rallyhealth
				.transform(new AgentBuilder.Transformer() {
					@Override
					public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) {
						return builder.visit(Advice.to(HelloWorldAdvice.class).on(isMethod()));
					}
				}).installOn(inst);
	}

	public static void agentmain(String agentOps, Instrumentation inst) {
		System.out.println("Agent main");
	}
}

class HelloWorldAdvice {

	@Advice.OnMethodEnter
	static void onEnter() {
		System.out.println("method started lol");
	}
}


// Notes
// Advice.AllArguments gets me all the args, not horrible
// Advice.Returns or something like that gets me returned value


// Questions
// for some reason only does retransformation for advice on the first class :shrugs:


// Flow
// instrument play filter (maybe even make one on the fly and attach to all all controllers?) to grab debug=True and stash into ThreadLocal
// whenever new threads are spawned, we have to check if ThreadLocal has debug=true, if so we need to copy into new thread
// in each method with prefix of com.rallyhealth we print method name and args on enter, and print return value on exit



