package ru.uporov.d.android.common.exception

import ru.uporov.d.android.common.InjectionRoot

class MoreThanOneInjectionRootException(): RuntimeException("${InjectionRoot::class.simpleName} cannot be used on more than one classes")