package com;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class InterceptorEnhancer {

    public <T> Object enhanceJDKProxy(T obj, Object... interceptores) {
        return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                obj.getClass().getInterfaces(),
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        JavaInterfaceInvocationContext context = new JavaInterfaceInvocationContext(obj, method, args);
                        ChainableInvocationContext chainContext = new ChainableInvocationContext(context, interceptores);
                        return chainContext.proceed();
                    }
                });
    }
}
