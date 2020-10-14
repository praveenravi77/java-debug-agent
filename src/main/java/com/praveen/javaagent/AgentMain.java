package com.praveen.javaagent;

import java.lang.instrument.Instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
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
			.with(AgentBuilder.Listener.StreamWriting.toSystemOut())

            // filter for the classes we actually want to look at
            .type(named("play.api.mvc.Action"))
            .transform(
                new AgentBuilder.Transformer() {

                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                        // probably better to compose new AgentBuilder.Transformer.ForAdvice https://github.com/raphw/byte-buddy/blob/master/byte-buddy-dep/src/main/java/net/bytebuddy/asm/Advice.java#L149-L153
                        return builder
                                .visit(
                                    Advice
                                        .to(LoggingAdvice.class)
                                        .on(named("apply"))
                                );
                    }
                }
            )
            .type(not(named("play.api.mvc.Action")).and(nameStartsWith("com.rallyhealth")))
            .transform(
                    new AgentBuilder.Transformer() {
                        @Override
                        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                            return builder
                                    .visit(
                                        Advice
                                            .to(LoggingAdvice.class)
                                            .on(isMethod())
                                    );
                        }
                    }
            )
            .installOn(inst);
	}

	public static void agentmain(String agentOps, Instrumentation inst) {
		System.out.println("Agent main");
	}
}

class DebugActionAdvice {
	@Advice.OnMethodEnter
	public static void onEnter(@Advice.Origin String origin, @Advice.AllArguments Object[] args) {
		DebugTree.addMethodCall(origin);
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void onExit(@Advice.Origin String origin) {
		System.out.println(DebugTree.getTree());
	}
}

class LoggingAdvice {

	@Advice.OnMethodEnter
	public static void onEnter(@Advice.Origin String origin, @Advice.AllArguments Object[] args) {
		System.out.println("STARTED -- " + origin);
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void onExit(@Advice.Origin String origin) {
		System.out.println("ENDED -- " + origin);
	}
}

// Notes
// Advice.AllArguments gets me all the args, not horrible
// Advice.Returns or something like that gets me returned value


// Questions
// Advice works as expects, just doesn't seem to work for toString


// Flow
// instrument play filter (maybe even make one on the fly and attach to all all controllers?) to grab debug=True and stash into ThreadLocal
// whenever new threads are spawned, we have to check if ThreadLocal has debug=true, if so we need to copy into new thread
// in each method with prefix of com.rallyhealth we print method name and args on enter, and print return value on exit
// preferred instead of printing would be cool if we could collect the results and change the response to have request, response, and trace with execution tree



