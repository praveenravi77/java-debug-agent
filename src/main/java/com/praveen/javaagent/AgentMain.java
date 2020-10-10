package com.praveen.javaagent;

import javassist.ClassPool;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.ClassFileTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class AgentMain {
	public static void premain(String agentOps, Instrumentation inst) {
		System.out.println("Agent premain");
		new AgentBuilder.Default()
				.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
				.with(new TransformLoggingListener())
				.type(ElementMatchers.nameStartsWith("com.praveen.testapp")) // This handles just grabbing the classes we care about, will probably be com.rallyhealth
				.transform(new AgentBuilder.Transformer() {
					@Override
					public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) {
						return builder
								.method(ElementMatchers.named("toString")) // ElementMatchers.named("toString")
								.intercept(MethodDelegation.to(ToStringInterceptor.class));
					}
				}).installOn(inst);
	}

	public static void agentmain(String agentOps, Instrumentation inst) {
		System.out.println("Agent main");
	}
}


class TransformLoggingListener implements AgentBuilder.Listener {

	@Override
	public void onError(
			final String typeName,
			final ClassLoader classLoader,
			final JavaModule module,
			final boolean loaded,
			final Throwable throwable) {

		System.out.println("Failed to handle transformation on classloader");
		System.out.println("Type: " + typeName);
		System.out.println("ClassLoader: " + classLoader);
		System.out.println("Error: " + throwable.getMessage());
	}

	@Override
	public void onTransformation(
			final TypeDescription typeDescription,
			final ClassLoader classLoader,
			final JavaModule module,
			final boolean loaded,
			final DynamicType dynamicType) {

		System.out.println("Transformed");
		System.out.println("Type: " + typeDescription.getName());
		System.out.println("ClassLoader: " + classLoader);
	}

	@Override
	public void onIgnored(
			final TypeDescription typeDescription,
			final ClassLoader classLoader,
			final JavaModule module,
			final boolean loaded) {

	}

	@Override
	public void onComplete(
			final String typeName,
			final ClassLoader classLoader,
			final JavaModule module,
			final boolean loaded) {

	}

	@Override
	public void onDiscovery(
			final String typeName,
			final ClassLoader classLoader,
			final JavaModule module,
			final boolean loaded) {

	}
}

