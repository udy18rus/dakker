package ru.uporov.d.android.apt

import androidx.lifecycle.LifecycleOwner
import com.squareup.kotlinpoet.*
import ru.uporov.d.android.common.exception.DakkerWasNotInitializedException

private const val FILE_NAME = "Dakker"

class DakkerBuilder(
    private val root: ClassName,
    private val nodesCores: Set<ClassName>
) {

    private val nodes: Set<ClassName> = nodesCores.map(ClassName::nodeClassName).toSet()
    private val nodesFields = setOf(root.nodeClassName()).union(nodes)
        .map { it.simpleName.decapitalize() to it }
        .toMap()

    fun build(): FileSpec {
        return FileSpec.builder(root.packageName, FILE_NAME)
            .addImport(
                "androidx.lifecycle",
                "LifecycleObserver", "LifecycleOwner", "OnLifecycleEvent", "Lifecycle"
            )
            .addType(
                TypeSpec.objectBuilder(FILE_NAME)
                    .nodesLateinitProperties()
                    .startDakkerFunction()
                    .nodesGetters()
                    .build()
            )
            .startScopesFunctions()
            .build()
    }

    private fun TypeSpec.Builder.nodesLateinitProperties() = apply {
        nodesFields.forEach {
            addProperty(
                PropertySpec.builder(
                    it.key,
                    it.value,
                    KModifier.PRIVATE,
                    KModifier.LATEINIT
                )
                    .mutable(true)
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.startDakkerFunction() = apply {
        addFunction(
            FunSpec.builder("startDakker")
                .receiver(root)
                .apply {
                    val codeBuilder = CodeBlock.builder()
                    nodesFields.forEach {
                        addParameter(ParameterSpec.builder(it.key, it.value).build())
                        codeBuilder.addStatement("$FILE_NAME.${it.key} = ${it.key}")
                    }
                    addCode(codeBuilder.build())
                }
                .build()
        )
    }

    private fun FileSpec.Builder.startScopesFunctions() = apply {
        nodesCores.forEach {
            addFunction(
                FunSpec.builder("startDakkerScope")
                    .addTypeVariable(TypeVariableName("T", it, LifecycleOwner::class.asClassName()))
                    .receiver(TypeVariableName("T"))
                    .addCode(
                        """
                    lifecycle.addObserver(object : LifecycleObserver {
                        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                        fun onDestroy() {
                            lifecycle.removeObserver(this)
                            Dakker.get${it.nodeName()}().trash()
                        }
                    })
                    """.trimIndent()
                    )
                    .build()
            )
        }
    }
//    fun <T> T.startScope() where T : MainActivity, T : LifecycleOwner {
//        lifecycle.addObserver(object : LifecycleObserver {
//            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
//            fun onDestroy() {
//                lifecycle.removeObserver(this)
//                mainActivityNode.trash()
//            }
//        })
//    }


    private fun TypeSpec.Builder.nodesGetters() = apply {
        nodesFields.forEach {
            addFunction(
                FunSpec.builder("get${it.key.capitalize()}")
                    .returns(it.value)
                    .addCode(
                        """
                        if (!::${it.key}.isInitialized) throw ${DakkerWasNotInitializedException::class.qualifiedName}()

                        return ${it.key}
                    """.trimIndent()
                    )
                    .build()
            )
        }
    }
}