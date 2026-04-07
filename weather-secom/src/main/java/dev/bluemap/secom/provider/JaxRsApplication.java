// src/main/java/dev/bluemap/secom/provider/JaxRsApplication.java

package dev.bluemap.secom.provider;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class JaxRsApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(LocalDateTimeParamConverterProvider.class);
        return classes;
    }
}