/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.pocket.data

import mozilla.components.service.pocket.helpers.assertConstructorsVisibility
import org.junit.Test
import kotlin.reflect.KVisibility

class PocketGlobalVideoRecommendationTest {

    @Test // See constructor comment for details.
    fun `GIVEN a PocketGlobalVideoRecommendation THEN its constructors are internal`() {
        assertConstructorsVisibility(PocketGlobalVideoRecommendation::class, KVisibility.INTERNAL)
    }

    @Test // See PocketGlobalVideoRecommendation constructor for details.
    fun `GIVEN an Author THEN its constructors are internal`() {
        assertConstructorsVisibility(PocketGlobalVideoRecommendation.Author::class, KVisibility.INTERNAL)
    }
}
