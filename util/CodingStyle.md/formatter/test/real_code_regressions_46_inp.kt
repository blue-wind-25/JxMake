/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Foo {
    public fun addProperties(
        propertySpecs: Iterable<PropertySpec>
    ): T = apply { propertySpecs.map(::addProperty) }

    override fun copy(
        nullable: Boolean,
        annotations: List<AnnotationSpec>,
        tags: Map<KClass<*>, Any>,
    ): LambdaTypeName {
        return copy(nullable, annotations, this.isSuspending, tags)
    }
}
