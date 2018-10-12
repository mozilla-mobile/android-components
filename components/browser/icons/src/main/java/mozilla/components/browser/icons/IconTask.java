/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import mozilla.components.browser.icons.loader.IconLoader;
import mozilla.components.browser.icons.preparation.Preparer;
import mozilla.components.browser.icons.processing.Processor;
import mozilla.components.support.base.log.logger.Logger;
import mozilla.components.support.utils.ThreadUtils;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Task that will be run by the IconRequestExecutor for every icon request.
 */
/* package-private */ class IconTask implements Callable<IconResponse> {
    private static final String LOGTAG = "Gecko/IconTask";
    private static final boolean DEBUG = false;

    private final List<Preparer> preparers;
    private final List<IconLoader> loaders;
    private final List<Processor> processors;
    private final IconLoader generator;
    private final IconRequest request;

    /* package-private */ IconTask(
            @NonNull IconRequest request,
            @NonNull List<Preparer> preparers,
            @NonNull List<IconLoader> loaders,
            @NonNull List<Processor> processors,
            @NonNull IconLoader generator) {
        this.request = request;
        this.preparers = preparers;
        this.loaders = loaders;
        this.processors = processors;
        this.generator = generator;
    }

    @Override
    public IconResponse call() {
        try {
            logRequest(request);

            prepareRequest(request);

            if (request.shouldPrepareOnly()) {
                // This request should only be prepared but not load an actual icon.
                return null;
            }

            final IconResponse response = loadIcon(request);

            if (response != null) {
                processIcon(request, response);
                executeCallback(request, response);

                logResponse(response);

                return response;
            }
        } catch (InterruptedException e) {
            Logger.Companion.debug("IconTask was interrupted", e);

            // Clear interrupt thread.
            Thread.interrupted();
        } catch (Throwable e) {
            handleException(e);
        }

        return null;
    }

    /**
     * Check if this thread was interrupted (e.g. this task was cancelled). Throws an InterruptedException
     * to stop executing the task in this case.
     */
    private void ensureNotInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Task has been cancelled");
        }
    }

    private void executeCallback(IconRequest request, final IconResponse response) {
        final IconCallback callback = request.getCallback();

        if (callback != null) {
            if (request.shouldRunOnBackgroundThread()) {
                ThreadUtils.postToBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onIconResponse(response);
                    }
                });
            } else {
                ThreadUtils.postToUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onIconResponse(response);
                    }
                });
            }
        }
    }

    private void prepareRequest(IconRequest request) throws InterruptedException {
        for (Preparer preparer : preparers) {
            ensureNotInterrupted();

            preparer.prepare(request);

            logPreparer(request, preparer);
        }
    }

    private IconResponse loadIcon(IconRequest request) throws InterruptedException {
        while (request.hasIconDescriptors()) {
            for (IconLoader loader : loaders) {
                ensureNotInterrupted();

                IconResponse response = loader.load(request);

                logLoader(request, loader, response);

                if (response != null) {
                    return response;
                }
            }

            request.moveToNextIcon();
        }

        return generator.load(request);
    }

    private void processIcon(IconRequest request, IconResponse response) throws InterruptedException {
        for (Processor processor : processors) {
            ensureNotInterrupted();

            processor.process(request, response);

            logProcessor(processor);
        }
    }

    private void handleException(final Throwable t) {
        if (DEBUG) {
        // TODO: Confirm that this new logic should be.
        //if (AppConstants.NIGHTLY_BUILD) {
            // We want to be aware of problems: Let's re-throw the exception on the main thread to
            // force an app crash. However we only do this in Nightly builds. Every other build
            // (especially release builds) should just carry on and log the error.
            ThreadUtils.postToUiThread(new Runnable() {
                @Override
                public void run() {
                    throw new RuntimeException("Icon task thread crashed", t);
                }
            });
        } else {
            Logger.Companion.error("Icon task crashed", t);
        }
    }

    private boolean shouldLog() {
        // Do not log anything if debugging is disabled and never log anything in a non-nightly build.
        // TODO: Confirm that this new logic should be.
        //return DEBUG && AppConstants.NIGHTLY_BUILD;
        return DEBUG;
    }

    private void logPreparer(IconRequest request, Preparer preparer) {
        if (!shouldLog()) {
            return;
        }

        Logger.Companion.debug(String.format("  PREPARE %s" + " (%s)",
                preparer.getClass().getSimpleName(),
                request.getIconCount()), null);
    }

    private void logLoader(IconRequest request, IconLoader loader, IconResponse response) {
        if (!shouldLog()) {
            return;
        }

        Logger.Companion.debug(String.format("  LOAD [%s] %s : %s",
                response != null ? "X" : " ",
                loader.getClass().getSimpleName(),
                request.getBestIcon().getUrl()), null);
    }

    private void logProcessor(Processor processor) {
        if (!shouldLog()) {
            return;
        }

        Logger.Companion.debug("  PROCESS " + processor.getClass().getSimpleName(), null);
    }

    private void logResponse(IconResponse response) {
        if (!shouldLog()) {
            return;
        }

        final Bitmap bitmap = response.getBitmap();

        Logger.Companion.debug(String.format("=> ICON: %sx%s", bitmap.getWidth(), bitmap.getHeight()), null);
    }

    private void logRequest(IconRequest request) {
        if (!shouldLog()) {
            return;
        }

        Logger.Companion.debug(String.format("REQUEST (%s) %s",
                request.getIconCount(),
                request.getPageUrl()), null);
    }
}
