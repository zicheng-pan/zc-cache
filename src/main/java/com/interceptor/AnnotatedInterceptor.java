package com.interceptor;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static java.lang.String.format;

public abstract class AnnotatedInterceptor<A extends Annotation> {

    private static final Class<? extends Annotation> INTERCEPTOR_ANNOTATION_TYPE = Interceptor.class;


    /**
     * @throws IllegalArgumentException If the implementation does not annotate {@link Interceptor @Interceptor} or
     *                                  the generic parameter type does not be specified.
     */
    public AnnotatedInterceptor() throws IllegalArgumentException {
        if (!getClass().isAnnotationPresent(INTERCEPTOR_ANNOTATION_TYPE)) {
            throw new IllegalArgumentException(
                    format("The Interceptor class[%s] must annotate %s", getClass(), INTERCEPTOR_ANNOTATION_TYPE));
        }
    }

    protected <A extends Annotation> Annotation findAnnotation(Method method, Class<A> bindingAnnotationType) {
        Annotation annotation = method.getAnnotation(bindingAnnotationType);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(bindingAnnotationType);
        }
        return (A) annotation;
    }

    protected abstract Object execute(InvocationContext context, A bindingAnnotation) throws Throwable;


    @AroundInvoke
    public final Object execute(InvocationContext context) throws Throwable {
        Type parameterizedType = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        Class<A> typeClass = (Class<A>) parameterizedType;
        Class<A> annotationType = null;
        if (!typeClass.isAnnotation()) {
            System.err.println(format("The annotationType[%s] should be a n nnotation",
                    typeClass.getName()
            ));
            throw new RuntimeException("error");
        }
        annotationType = (Class<A>) typeClass;
        Annotation annotation = findAnnotation(context.getMethod(), annotationType);
        return execute(context, (A) annotation);

    }
}
