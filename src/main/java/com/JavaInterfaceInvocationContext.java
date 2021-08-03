package com;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class JavaInterfaceInvocationContext implements InvocationContext {

    // target 为需要代理的对象
    private Object target;

    private Method method;

    private Object[] params;

    private final Map<String, Object> contextData;

    public JavaInterfaceInvocationContext(Object target, Method method, Object... params) {
        this.target = target;
        this.method = method;
        this.params = params;
        this.contextData = new HashMap<>();
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object getTimer() {
        throw new UnsupportedOperationException("ReflectiveMethodInvocationContext does not support to get the Constructor!");
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Constructor<?> getConstructor() {
        throw new UnsupportedOperationException("ReflectiveMethodInvocationContext does not support to get the Constructor!");
    }

    @Override
    public Object[] getParameters() {
        return params;
    }

    @Override
    public void setParameters(Object[] params) {
        this.params = params;
    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData;
    }

    @Override
    public Object proceed() throws Exception {
        return method.invoke(target, params);
    }
}
