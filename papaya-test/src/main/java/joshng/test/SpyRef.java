package joshng.test;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
* User: josh
* Date: 12/27/11
* Time: 5:42 PM
*/
public class SpyRef<T> {
    T spy;
    @Nullable
    private Consumer<T> spyInitializer;

    public static <T> SpyRef<T> of(Class<T> valueClass) {
        return of(valueClass, null);
    }

    public static <T> SpyRef<T> of(Class<T> valueClass, @Nullable Consumer<T> spyInitializer) {
        return new SpyRef<T>(valueClass, spyInitializer);
    }

    private final Class<T> valueClass;

    public SpyRef(Class<T> valueClass, @Nullable Consumer<T> spyInitializer) {
        this.valueClass = valueClass;
        this.spyInitializer = spyInitializer;
    }

    @Nullable
    public Consumer<T> getSpyInitializer() {
        return spyInitializer;
    }

    public void setSpyInitializer(@Nullable Consumer<T> spyInitializer) {
        this.spyInitializer = spyInitializer;
    }

    public Stubber spyReturnValue() {
        return Mockito.doAnswer(new Answer<T>() {
            public T answer(InvocationOnMock invocation) throws Throwable {
                return setupSpy(invocation.callRealMethod());
            }
        });
    }

    public Stubber spyParameter(final int parameterIdx) {
        return Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                arguments[parameterIdx] = setupSpy(arguments[parameterIdx]);
                return invocation.callRealMethod();
            }
        });
    }

    protected T setupSpy(Object realValue) {
        spy = Mockito.spy(valueClass.cast(realValue));
        if (spyInitializer != null) spyInitializer.accept(spy);
        return spy;
    }

    public T getSpy() {
        return spy;
    }

    public T verify() {
        return Mockito.verify(spy);
    }

}
