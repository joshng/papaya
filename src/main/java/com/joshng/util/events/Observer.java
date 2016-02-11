package com.joshng.util.events;

import java.lang.annotation.*;

/**
 * User: josh
 * Date: 6/5/14
 * Time: 12:51 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Observer {
}
