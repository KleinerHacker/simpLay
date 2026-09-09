/*
 * Copyright (c) KleinerHacker alias Pfeiffer C Soft 2026.
 * This work is licensed under the Apache License, Version 2.0.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, this software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations.
 */

package org.pcsoft.framework.simplay.fx

import java.util.concurrent.CompletableFuture
import javafx.application.Platform
import org.junit.jupiter.api.BeforeAll
import org.testfx.api.FxToolkit

/**
 * Common base for the headless JavaFX tests. Boots the JavaFX toolkit once via TestFX/Monocle (the
 * `testfx.headless` / Monocle system properties are set by the Gradle build) and offers a helper to
 * run code on the JavaFX application thread.
 */
abstract class JavaFxTestBase {

    /** Runs [block] on the JavaFX application thread and returns its result. */
    protected fun <T> onFxThread(block: () -> T): T {
        if (Platform.isFxApplicationThread()) return block()
        val future = CompletableFuture<T>()
        Platform.runLater {
            try {
                future.complete(block())
            } catch (t: Throwable) {
                future.completeExceptionally(t)
            }
        }
        return future.get()
    }

    companion object {

        /** Starts the headless toolkit before any test in a subclass runs. */
        @JvmStatic
        @BeforeAll
        fun bootToolkit() {
            FxToolkit.registerPrimaryStage()
        }
    }
}
