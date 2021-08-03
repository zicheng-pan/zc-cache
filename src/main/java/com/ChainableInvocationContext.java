package com;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ChainableInvocationContext implements InvocationContext {

    private final InvocationContext context;

    private final Object[] interceptors; // @Interceptor class instances

    private final int length;

    private final Map<Object, Method> indexedAroundInvokeMethods;  //将Intercepter对象和它所对应的 AroundInvoke 方法对应起来

    private int pos; // position

    public ChainableInvocationContext(InvocationContext context, Object... interceptors) {
        this.context = context;
        this.interceptors = interceptors;
        this.length = interceptors.length;
        this.indexedAroundInvokeMethods = initInvokeMethod();
        this.pos = 0;
    }

    private Map<Object, Method> initInvokeMethod() {
        Map<Object, Method> objectMethodHashMap = new HashMap<>();
        for (int i = 0; i < length; i++) {

            Object interceptor = interceptors[i];
            Optional<Method> optionalMethod = findAroundInvokeMethod(interceptor);
            optionalMethod.ifPresent(method -> {
                objectMethodHashMap.put(interceptor, method);
            });
        }
        return objectMethodHashMap;
    }

    // 用@AroundInvoke 来修饰的方法 一般我们这里的一个interceptor类中只有一个exec方法
    private Optional<Method> findAroundInvokeMethod(Object interceptor) {
        return Stream.of(interceptor.getClass().getMethods()).filter(method -> {
            int mods = method.getModifiers();
            return method.isAnnotationPresent(AroundInvoke.class) && method.getParameterCount() == 1 //只有一个参数并且参数是InvocationContext
                    && InvocationContext.class.isAssignableFrom(method.getParameterTypes()[0])
                    && !Modifier.isStatic(mods);
        }).findFirst();
    }


    @Override
    public Object getTarget() {
        return context.getTarget();
    }

    @Override
    public Object getTimer() {
        return context.getTimer();
    }

    @Override
    public Method getMethod() {
        return context.getMethod();
    }

    @Override
    public Constructor<?> getConstructor() {
        return context.getConstructor();
    }

    @Override
    public Object[] getParameters() {
        return context.getParameters();
    }

    @Override
    public void setParameters(Object[] params) {
        context.setParameters(params);
    }

    @Override
    public Map<String, Object> getContextData() {
        return context.getContextData();
    }

    @Override
    public Object proceed() throws Exception {
        if (pos < length) {
            Object object = interceptors[pos];
            Method method = indexedAroundInvokeMethods.get(object);
            pos = pos + 1;
            return method.invoke(object, this);
        } else {
            return context.proceed();
        }
    }
}
