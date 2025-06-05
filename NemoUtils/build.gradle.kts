cloudstream {
    isLibrary = true
}

subprojects {
    tasks.named("compileDex") {
        enabled = false
    }
}
