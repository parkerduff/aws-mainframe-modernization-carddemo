package com.carddemo.accountview.service;

/**
 * Result of an Account View request.
 *
 * @param status        outcome classification
 * @param errorMessage  message shown in CCARD-ERROR-MSG / ERRMSGO on failure;
 *                      empty string on success (legacy clears WS-RETURN-MSG)
 * @param infoMessage   message shown in INFOMSGO (the legacy program always
 *                      leaves the input prompt active on this map)
 * @param accountDetails populated only when {@link #status} is
 *                      {@link AccountViewStatus#SUCCESS}; otherwise {@code null}
 */
public record AccountViewResult(
        AccountViewStatus status,
        String errorMessage,
        String infoMessage,
        AccountDetails accountDetails) {

    public boolean isSuccess() {
        return status == AccountViewStatus.SUCCESS;
    }
}
