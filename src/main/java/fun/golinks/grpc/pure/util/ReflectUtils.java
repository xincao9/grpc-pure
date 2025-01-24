package fun.golinks.grpc.pure.util;

import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class ReflectUtils {

    public static final String CGLIB_CLASS_SEPARATOR = "$$";

    /**
     * 获取对象的Class
     */
    @Nonnull
    public static Class<?> getUserClass(Object instance) {
        return getUserClass(instance.getClass());
    }

    /**
     * 获取原始Class
     */
    public static Class<?> getUserClass(Class<?> clazz) {
        if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }
        return clazz;
    }

    /**
     * 获取对象字段标记的注解对象
     *
     * @param object
     *            对象
     * @param annotationClass
     *            注解类class
     * @param <T>
     *            注解类
     * 
     * @return 注解对象
     */
    public static <T extends Annotation> T getFieldAnnotation(Object object, Class<T> annotationClass) {
        Class<?> clazz = ReflectUtils.getUserClass(object);
        Field[] fields = clazz.getDeclaredFields();
        if (ArrayUtils.isNotEmpty(fields)) {
            for (Field field : fields) {
                if (field.isAnnotationPresent(annotationClass)) {
                    return field.getAnnotation(annotationClass);
                }
            }
        }
        return null;
    }
}
