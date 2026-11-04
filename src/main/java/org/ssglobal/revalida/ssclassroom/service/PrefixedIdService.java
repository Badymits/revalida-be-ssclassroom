package org.ssglobal.revalida.ssclassroom.service;

import org.hibernate.annotations.IdGeneratorType;
import java.lang.annotation.*;

@IdGeneratorType(PrefixedIdGeneratorService.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
   
public @interface PrefixedIdService {
       String prefix();
   }