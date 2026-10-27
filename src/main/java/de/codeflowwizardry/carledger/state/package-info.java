/**
 * The tokens of the logins, kept in Infinispan instead of the session cookie
 * ({@link de.codeflowwizardry.carledger.state.InfinispanTokenStateManager}):
 * the cookie only holds a key, so it stays small and several instances share
 * the sessions.
 */
package de.codeflowwizardry.carledger.state;
