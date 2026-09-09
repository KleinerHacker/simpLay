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

package org.pcsoft.framework.simplay.swing

import javax.swing.plaf.ComponentUI

/**
 * The pluggable Look-and-Feel delegate type of [PaperSheetView], the way `ScrollBarUI` or `TreeUI`
 * are for their components. A Look-and-Feel provides a subclass and registers it under the UI class
 * id `"PaperSheetViewUI"`; the built-in default is [BasicPaperSheetUI].
 *
 * The delegate owns the measuring, the vertical scroll bar, the pointer / mouse routing and the
 * viewport painting; it is the Swing counterpart of the `fx` module's `PaperSheetViewSkin`.
 */
abstract class PaperSheetUI : ComponentUI()
