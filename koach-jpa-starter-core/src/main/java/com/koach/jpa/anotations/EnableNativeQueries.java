package com.koach.jpa.anotations;
import com.koach.jpa.registers.NativeQueryRepositoryRegistrar;
import org.springframework.context.annotation.Import;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(NativeQueryRepositoryRegistrar.class)
@SuppressWarnings("unused")
public @interface EnableNativeQueries {
    String[] basePackages() default {};
}
