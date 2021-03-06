package com.squareup.burst;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

import static com.squareup.burst.Util.checkNotNull;
import static java.util.Collections.unmodifiableList;

public final class BurstJUnit4 extends Suite {
  public BurstJUnit4(Class<?> cls) throws InitializationError {
    super(cls, explode(cls));
  }

  static List<Runner> explode(Class<?> cls) throws InitializationError {
    checkNotNull(cls, "cls");

    TestClass testClass = new TestClass(cls);
    List<FrameworkMethod> testMethods = testClass.getAnnotatedMethods(Test.class);

    List<FrameworkMethod> burstMethods = new ArrayList<>(testMethods.size());
    for (FrameworkMethod testMethod : testMethods) {
      Method method = testMethod.getMethod();
      for (Enum<?>[] methodArgs : Burst.explodeArguments(method)) {
        burstMethods.add(new BurstMethod(method, methodArgs));
      }
    }

    Constructor<?> constructor = findConstructor(cls);
    Enum<?>[][] constructorArgsList = Burst.explodeArguments(constructor);
    List<Runner> burstRunners = new ArrayList<>(constructorArgsList.length);
    for (Enum<?>[] constructorArgs : constructorArgsList) {
      burstRunners.add(new BurstRunner(cls, constructor, constructorArgs, burstMethods));
    }

    return unmodifiableList(burstRunners);
  }

  /** Locate the default or public constructor for a test class. */
  static Constructor<?> findConstructor(Class<?> cls) {
    checkNotNull(cls, "cls");

    Constructor<?>[] constructors = cls.getConstructors();
    if (constructors.length == 1) {
      return constructors[0];
    }
    throw new IllegalStateException(cls.getName() + " requires a single public constructor.");
  }

  static String nameWithArguments(String name, Enum<?>[] arguments) {
    if (arguments.length == 0) {
      return name;
    }
    return name + '[' + Burst.friendlyName(arguments) + ']';
  }
}
