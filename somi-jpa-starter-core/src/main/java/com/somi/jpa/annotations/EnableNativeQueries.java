package com.somi.jpa.annotations;
import com.somi.jpa.registers.NativeQueryRepositoryRegistrar;
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
