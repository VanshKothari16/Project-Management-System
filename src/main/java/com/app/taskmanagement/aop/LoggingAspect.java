package com.app.taskmanagement.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * This is the "hidden camera" for the whole application. It watches every
 * method inside controller/, services/, and repository/ and writes down:
 *   1) When a method starts, and what values (arguments) were passed in.
 *   2) When a method finishes successfully, what it returned, and how
 *      long it took (in milliseconds).
 *   3) If a method throws an error, exactly which method failed and why.
 *
 * You do not need to touch any existing class for this to work - just
 * having this file in the project, plus the spring-boot-starter-aop
 * dependency in pom.xml, is enough for Spring to wire it up automatically.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * A "Pointcut" is simply a RULE that says WHICH methods the camera
     * should watch. This one says: "every method inside the controller,
     * services, or repository packages, and any of their sub-packages."
     */
    @Pointcut("within(com.app.taskmanagement.controller..*) " +
            "|| within(com.app.taskmanagement.services..*) " +
            "|| within(com.app.taskmanagement.repository..*)")
    public void applicationPackagePointcut() {
        // Intentionally empty - this method only exists to "name" the rule above.
    }

    /**
     * @Around means "run my code BEFORE the real method runs, AND run my
     * code AFTER it finishes." Think of it as a sandwich:
     *      [my code] -> [the real method runs] -> [my code again]
     */
    @Around("applicationPackagePointcut()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("ENTER {}.{}() with arguments = {}",
                className, methodName, Arrays.toString(args));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long timeTaken = System.currentTimeMillis() - startTime;

            log.info("EXIT  {}.{}() - took {} ms - returned = {}",
                    className, methodName, timeTaken, result);

            return result;

        } catch (Throwable ex) {
            long timeTaken = System.currentTimeMillis() - startTime;
            log.error("ERROR in {}.{}() after {} ms - {}: {}",
                    className, methodName, timeTaken,
                    ex.getClass().getSimpleName(), ex.getMessage());
            // Re-throw so the existing ExceptionHandler still gets to
            // turn this into a clean JSON error response for the user.
            throw ex;
        }
    }
}
