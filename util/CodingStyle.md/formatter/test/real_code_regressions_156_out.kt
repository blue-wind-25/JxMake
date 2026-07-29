/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

fun resolveProjectDependencyComponentsWithArtifacts(
    resolvedArtifacts: Set<String>,
    allComponentsProvider: Set<String>,
    rootComponentProvider: String,
    removeAttributesNamed: Set<String> = setOf(
        // The status is inferred from the presence of "-SNAPSHOT" in this version. It should have no effect on resolution, but makes testing against snapshot versions painful
        "org.gradle.status",
    ),
): Map<String, String>
{
    return mapOf()
}

internal fun KClassImpl<*>.isVisibleAsFunctionInCurrentClass(function: JavaKNamedFunction): Boolean
{
    if( getPropertyNamesCandidatesByAccessorName(
        Name.identifier(function.name)
    ).any { propertyName ->
            getPropertiesFromSupertypes( propertyName.asString() ).any { property ->
                doesClassOverrideProperty(property) { accessorName ->
                    if(function.name == accessorName)
                        listOf(function)
                    else {
                        // K1 code also searched in supertypes (see searchMethodsInSupertypesWithoutBuiltinMagic), but it seems useful
                        // only for mapped builtins and their subtypes, so will be handled separately in KT-85727
                        getDeclaredNonStaticMethodsFromJavaClass().filter { it.name == accessorName }
                    }
                } && ( property is KMutableProperty<*> || !JvmAbi.isSetterName(function.name) )
            }
        } ) return false

    return true
}
