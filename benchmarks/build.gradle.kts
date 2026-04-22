plugins {
    alias(libs.plugins.jmh)
}

dependencies {
    jmh(project(":command-core"))
    jmh(libs.jmh.core)
    jmh(libs.jmh.generator.annprocess)
}

jmh {
    jmhVersion.set(libs.versions.jmh.get())
    warmupIterations.set(2)
    iterations.set(3)
    fork.set(1)
    benchmarkMode.set(listOf("thrpt"))
    timeUnit.set("ms")
}
