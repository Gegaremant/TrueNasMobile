package com.gegaremant.truenasmobile.ui

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * The ViewModel owner for the shared dashboard state.
 *
 * Home, Storage and Performance are three views of one dataset, and they used to
 * build a `HomeViewModel` each - three identical eight-RPC batches on first open
 * and three 30-second connectivity polls. They now share one instance, which
 * needs an owner that outlives a tab switch but not a sign-out.
 *
 * **The trap this function exists to name.** The app runs two NavHosts that are
 * siblings, not parent and child:
 *
 *  - the root one, declared in `MainActivity`, holding the pre-login and
 *    account-level screens - `login`, `account_switcher`, `settings`,
 *    `change_password`, and `main`, which hosts MainScreen itself;
 *  - the inner one, declared inside `MainScreen`, holding the tabs and their
 *    detail screens, starting at `home`.
 *
 * Neither graph can see the other's destinations, so the owner has to be asked of
 * the **root** controller. Asking the inner one throws
 * `IllegalArgumentException: Navigation destination that matches route main
 * cannot be found` - during composition, before the first frame, so the app just
 * closes with no error shown. That shipped once.
 *
 * Kept as a named function with the root controller as its only parameter so the
 * call site cannot quietly pass the wrong one, and so the behaviour can be
 * exercised for real: `NavHostOwnershipRuntimeTest` builds both graphs and calls
 * this function against them.
 */
internal fun dashboardViewModelOwner(rootNavController: NavController): NavBackStackEntry =
    rootNavController.getBackStackEntry(Screen.Main.route)
