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

package org.pcsoft.framework.simplay.uicommon

/**
 * The four sides of a single page a page decoration of a `PaperSheetView` (fx and swing) may be
 * anchored to, in the component area around the page (inside `outerMargin`/`pageGap`), not in the
 * page content itself.
 */
enum class PageEdge {

    /** The top edge of the page. */
    TOP,

    /** The bottom edge of the page. */
    BOTTOM,

    /** The left edge of the page. */
    LEFT,

    /** The right edge of the page. */
    RIGHT,
}
