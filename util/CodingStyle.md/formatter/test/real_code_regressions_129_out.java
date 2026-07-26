/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class M {

    private static JavaType.Method newMethodType(
        String    type,
        String    method,
        String... parameterTypes
    )
    {
        List<JavaType> parameterTypeList = Stream.of(
            parameterTypes
        ).map( name-> { JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(name) ; if(primitive != null) return primitive ;
        else if( "int".equals(name) ) return JavaType.Primitive.Int ;
        else if( "long".equals(name) ) return JavaType.Primitive.Long ;
        else return JavaType.ShallowClass.build(name) ; } ).map(JavaType.class::cast).toList();

        return parameterTypeList;
    }

} // class M
