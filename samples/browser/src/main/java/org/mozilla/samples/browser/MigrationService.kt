package org.mozilla.samples.browser

import mozilla.components.support.migration.AbstractMigrationService
import mozilla.components.support.migration.state.MigrationStore
import org.mozilla.samples.browser.ext.components

/**
 * Something.
 */
class MigrationService : AbstractMigrationService() {
    override val migrator by lazy { components.migrator }
    override val store: MigrationStore by lazy { components.migrationStore }
}
