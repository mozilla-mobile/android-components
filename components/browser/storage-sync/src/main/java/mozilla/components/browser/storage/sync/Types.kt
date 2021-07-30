/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("MatchingDeclarationName")

package mozilla.components.browser.storage.sync

import mozilla.appservices.places.BookmarkFolder
import mozilla.appservices.places.BookmarkItem
import mozilla.appservices.places.BookmarkSeparator
import mozilla.appservices.places.BookmarkTreeNode
import mozilla.appservices.places.SyncAuthInfo
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.concept.storage.DocumentType
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.concept.storage.HistoryMetadata
import mozilla.components.concept.storage.HistoryMetadataKey
import mozilla.components.concept.storage.HistoryMetadataObservation
import mozilla.components.concept.storage.TopFrecentSiteInfo
import mozilla.components.concept.storage.VisitInfo
import mozilla.components.concept.storage.VisitType

// We have type definitions at the concept level, and "external" types defined within Places.
// In practice these two types are largely the same, and this file is the conversion point.

/**
 * Conversion from our SyncAuthInfo into its "native" version used at the interface boundary.
 */
internal fun mozilla.components.concept.sync.SyncAuthInfo.into(): SyncAuthInfo {
    return SyncAuthInfo(
        kid = this.kid,
        fxaAccessToken = this.fxaAccessToken,
        syncKey = this.syncKey,
        tokenserverURL = this.tokenServerUrl
    )
}

/**
 * Conversion from a generic [FrecencyThresholdOption] into its richer comrade within the 'places' lib.
 */
internal fun FrecencyThresholdOption.into() = when (this) {
    FrecencyThresholdOption.NONE -> mozilla.appservices.places.FrecencyThresholdOption.NONE
    FrecencyThresholdOption.SKIP_ONE_TIME_PAGES ->
        mozilla.appservices.places.FrecencyThresholdOption.SKIP_ONE_TIME_PAGES
}

/**
 * Conversion from a generic [VisitType] into its richer comrade within the 'places' lib.
 */
internal fun VisitType.into() = when (this) {
    VisitType.NOT_A_VISIT -> mozilla.appservices.places.VisitType.UPDATE_PLACE
    VisitType.LINK -> mozilla.appservices.places.VisitType.LINK
    VisitType.RELOAD -> mozilla.appservices.places.VisitType.RELOAD
    VisitType.TYPED -> mozilla.appservices.places.VisitType.TYPED
    VisitType.BOOKMARK -> mozilla.appservices.places.VisitType.BOOKMARK
    VisitType.EMBED -> mozilla.appservices.places.VisitType.EMBED
    VisitType.REDIRECT_PERMANENT -> mozilla.appservices.places.VisitType.REDIRECT_PERMANENT
    VisitType.REDIRECT_TEMPORARY -> mozilla.appservices.places.VisitType.REDIRECT_TEMPORARY
    VisitType.DOWNLOAD -> mozilla.appservices.places.VisitType.DOWNLOAD
    VisitType.FRAMED_LINK -> mozilla.appservices.places.VisitType.FRAMED_LINK
}

internal fun mozilla.appservices.places.VisitType.into() = when (this) {
    mozilla.appservices.places.VisitType.UPDATE_PLACE -> VisitType.NOT_A_VISIT
    mozilla.appservices.places.VisitType.LINK -> VisitType.LINK
    mozilla.appservices.places.VisitType.RELOAD -> VisitType.RELOAD
    mozilla.appservices.places.VisitType.TYPED -> VisitType.TYPED
    mozilla.appservices.places.VisitType.BOOKMARK -> VisitType.BOOKMARK
    mozilla.appservices.places.VisitType.EMBED -> VisitType.EMBED
    mozilla.appservices.places.VisitType.REDIRECT_PERMANENT -> VisitType.REDIRECT_PERMANENT
    mozilla.appservices.places.VisitType.REDIRECT_TEMPORARY -> VisitType.REDIRECT_TEMPORARY
    mozilla.appservices.places.VisitType.DOWNLOAD -> VisitType.DOWNLOAD
    mozilla.appservices.places.VisitType.FRAMED_LINK -> VisitType.FRAMED_LINK
}

internal fun mozilla.appservices.places.VisitInfo.into(): VisitInfo {
    return VisitInfo(
        url = this.url,
        title = this.title,
        visitTime = this.visitTime,
        visitType = this.visitType.into()
    )
}

internal fun mozilla.appservices.places.TopFrecentSiteInfo.into(): TopFrecentSiteInfo {
    return TopFrecentSiteInfo(
        url = this.url,
        title = this.title
    )
}

internal fun BookmarkTreeNode.asBookmarkNode(): BookmarkNode {
    return when (this) {
        is BookmarkItem -> {
            BookmarkNode(
                BookmarkNodeType.ITEM,
                this.guid,
                this.parentGUID,
                this.position,
                this.title,
                this.url,
                this.dateAdded,
                null
            )
        }
        is BookmarkFolder -> {
            BookmarkNode(
                BookmarkNodeType.FOLDER,
                this.guid,
                this.parentGUID,
                this.position,
                this.title,
                null,
                this.dateAdded,
                this.children?.map(BookmarkTreeNode::asBookmarkNode)
            )
        }
        is BookmarkSeparator -> {
            BookmarkNode(
                BookmarkNodeType.SEPARATOR,
                this.guid,
                this.parentGUID,
                this.position,
                null,
                null,
                this.dateAdded,
                null
            )
        }
    }
}

internal fun HistoryMetadataKey.into(): mozilla.appservices.places.HistoryMetadataKey {
    return mozilla.appservices.places.HistoryMetadataKey(
        url = this.url,
        referrerUrl = this.referrerUrl,
        searchTerm = this.searchTerm
    )
}

internal fun mozilla.appservices.places.HistoryMetadataKey.into(): HistoryMetadataKey {
    return HistoryMetadataKey(
        url = this.url,
        referrerUrl = if (this.referrerUrl.isNullOrEmpty()) { null } else { this.referrerUrl },
        searchTerm = if (this.searchTerm.isNullOrEmpty()) { null } else { this.searchTerm }
    )
}

internal fun mozilla.appservices.places.DocumentType.into(): DocumentType {
    return when (this) {
        mozilla.appservices.places.DocumentType.Regular -> DocumentType.Regular
        mozilla.appservices.places.DocumentType.Media -> DocumentType.Media
    }
}

internal fun mozilla.appservices.places.HistoryMetadata.into(): HistoryMetadata {
    // Protobuf doesn't support passing around `null` value, so these get converted to some defaults
    // as they go from Rust to Kotlin. E.g. an empty string in place of a `null`.
    // That means places.HistoryMetadata will never have `null` values.
    // But, we actually do want a real `null` value here - hence the explicit check.
    return HistoryMetadata(
        key = this.key.into(),
        title = if (this.title.isNullOrEmpty()) null else this.title,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        totalViewTime = this.totalViewTime,
        documentType = this.documentType.into()
    )
}

internal fun List<mozilla.appservices.places.HistoryMetadata>.into(): List<HistoryMetadata> {
    return map { it.into() }
}

internal fun DocumentType.into(): mozilla.appservices.places.DocumentType {
    return when (this) {
        DocumentType.Regular -> mozilla.appservices.places.DocumentType.Regular
        DocumentType.Media -> mozilla.appservices.places.DocumentType.Media
    }
}

internal fun HistoryMetadata.into(): mozilla.appservices.places.HistoryMetadata {
    return mozilla.appservices.places.HistoryMetadata(
        key = this.key.into(),
        title = this.title,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        totalViewTime = this.totalViewTime,
        documentType = this.documentType.into()
    )
}

internal fun HistoryMetadataObservation.into(): mozilla.appservices.places.HistoryMetadataObservation {
    return when (this) {
        is HistoryMetadataObservation.ViewTimeObservation -> {
            mozilla.appservices.places.HistoryMetadataObservation.ViewTimeObservation(
                viewTime = this.viewTime
            )
        }
        is HistoryMetadataObservation.DocumentTypeObservation -> {
            mozilla.appservices.places.HistoryMetadataObservation.DocumentTypeObservation(
                documentType = this.documentType.into()
            )
        }
    }
}
