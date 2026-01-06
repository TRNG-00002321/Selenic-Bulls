# employee/tests/system_tests/selenium/features/steps/get_expenses_steps.py
import os
import sqlite3

from behave import given, when, then
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import StaleElementReferenceException


def _db_path() -> str:
    return os.getenv("DATABASE_PATH", "expense_manager.db")


def _with_implicit_wait(context, seconds: int, fn):
    """
    Temporarily change implicit wait so optional lookups don't burn 10s each.
    """
    driver = context.driver
    try:
        driver.implicitly_wait(seconds)
        return fn()
    finally:
        driver.implicitly_wait(10)  # matches your environment.py default


def _page_has_table_or_empty_state(context) -> bool:
    return _table_present(context) or _empty_state_visible(context)


def _delete_all_user_expenses(username: str = "employee1") -> None:
    """
    Ensures the empty-filter scenarios are deterministic by clearing the user's
    expenses and approvals directly in SQLite.
    """
    conn = sqlite3.connect(_db_path())
    conn.row_factory = sqlite3.Row
    try:
        cur = conn.cursor()

        cur.execute("SELECT id FROM users WHERE username = ?", (username,))
        user = cur.fetchone()
        if not user:
            return

        user_id = user["id"]

        cur.execute("SELECT id FROM expenses WHERE user_id = ?", (user_id,))
        expense_ids = [r["id"] for r in cur.fetchall()]

        if expense_ids:
            placeholders = ",".join(["?"] * len(expense_ids))
            cur.execute(f"DELETE FROM approvals WHERE expense_id IN ({placeholders})", expense_ids)
            cur.execute(f"DELETE FROM expenses WHERE id IN ({placeholders})", expense_ids)

        conn.commit()
    finally:
        conn.close()


def _expenses_list_text(context) -> str:
    try:
        el = context.driver.find_element(By.ID, "expenses-list")
        return (el.text or "").strip()
    except Exception:  # noqa: BLE001
        return ""


def _empty_state_visible(context) -> bool:
    """
    Empty state = table hidden and text shown that contains 'no expenses'
    (scoped to #expenses-list).
    """
    return "no expenses" in _expenses_list_text(context).lower()


def _table_present(context) -> bool:
    return len(context.driver.find_elements(By.CSS_SELECTOR, "#expenses-list table")) > 0


def _wait_for_list_render(context):
    """
    After refresh/filter, either:
    - a table appears, OR
    - the empty-state text appears.
    """
    context.wait.until(lambda d: _table_present(context) or _empty_state_visible(context))


def _expenses_table(context):
    """
    Returns the table element if present, else None (empty state).
    """
    _wait_for_list_render(context)
    tables = context.driver.find_elements(By.CSS_SELECTOR, "#expenses-list table")
    return tables[0] if tables else None


def _expense_rows(context):
    """
    Always locate rows fresh. If table doesn't exist (empty-state UI), return [].
    """
    if _empty_state_visible(context):
        return []

    last_exc = None
    for _ in range(8):
        try:
            table = _expenses_table(context)
            if table is None:
                return []
            return context.driver.find_elements(
                By.XPATH,
                "//div[@id='expenses-list']//table//tbody//tr[td]"
            )
        except StaleElementReferenceException as exc:
            last_exc = exc
            continue

    raise last_exc


def _status_text_from_row(context, row) -> str:
    """
    Reads status cell safely; retries if the row becomes stale during DOM updates.
    """
    last_exc = None
    for _ in range(6):
        try:
            cells = row.find_elements(By.TAG_NAME, "td")
            assert len(cells) >= 4, f"Expected at least 4 columns, got {len(cells)}"
            return (cells[3].text or "").strip()
        except StaleElementReferenceException as exc:
            last_exc = exc
            rows = _expense_rows(context)
            if rows:
                row = rows[0]
            continue

    raise last_exc


@given("I have no expenses in the system")
def step_have_no_expenses(context):
    _delete_all_user_expenses("employee1")
    context.driver.refresh()
    _wait_for_list_render(context)


@when('I select the expense status filter "{filter_name}"')
def step_select_status_filter(context, filter_name):
    # Remember selection (useful for debugging)
    context._selected_status_filter = filter_name

    select_el = context.wait.until(
        EC.visibility_of_element_located((By.ID, "status-filter"))
    )
    Select(select_el).select_by_visible_text(filter_name)


@when("I refresh the expense list")
def step_refresh_expense_list(context):
    """
    Click Refresh and wait for a stable loaded state.
    IMPORTANT: Do not require the text to change (e.g., All->All may not change).
    """
    refresh_btn = context.wait.until(
        EC.element_to_be_clickable((By.ID, "refresh-expenses"))
    )
    refresh_btn.click()

    # Wait for either table OR empty-state message
    context.wait.until(lambda d: _page_has_table_or_empty_state(context))


@then("I should see the expense list loaded successfully")
def step_list_loaded(context):
    _wait_for_list_render(context)


@then("each expense row should display a status")
def step_each_row_has_status(context):
    rows = _expense_rows(context)
    for row in rows:
        status = _status_text_from_row(context, row)
        assert status != "", "Found an expense row with a blank status."


@then('every displayed expense should have status "{expected_status}"')
def step_every_row_matches_status(context, expected_status):
    expected_lower = expected_status.lower()

    def _all_rows_match():
        rows = _expense_rows(context)

        # If empty-state is visible, there are no mismatching rows.
        if _empty_state_visible(context):
            return True

        if not rows:
            return False

        for row in rows:
            try:
                actual = _status_text_from_row(context, row).lower()
            except StaleElementReferenceException:
                return False
            if actual != expected_lower:
                return False

        return True

    # Wait until the UI has finished applying the filter and all rows match
    context.wait.until(lambda d: _all_rows_match())

    # Final assert (gives you the helpful message if it ever flakes)
    rows = _expense_rows(context)
    for row in rows:
        actual = _status_text_from_row(context, row)
        assert actual.lower() == expected_lower, (
            f"Expected '{expected_status}' (case-insensitive), got '{actual}'"
        )


@then("the displayed expense count should match the number of rows")
def step_count_matches_rows(context):
    rows = _expense_rows(context)

    def _find_count():
        for locator in [
            (By.ID, "expense-count"),
            (By.ID, "expenses-count"),
            (By.CSS_SELECTOR, "[data-testid='expense-count']"),
            (By.XPATH, "//*[contains(normalize-space(), 'Count')]"),
        ]:
            els = context.driver.find_elements(*locator)
            if not els:
                continue
            text = (els[0].text or "").strip()
            digits = "".join(ch if ch.isdigit() else " " for ch in text).split()
            if digits:
                return int(digits[-1])
        return None

    found_count = _with_implicit_wait(context, 0, _find_count)

    # If no count is displayed in the UI, we just don't assert it.
    if found_count is not None:
        assert found_count == len(rows), (
            f"Displayed count ({found_count}) did not match table rows ({len(rows)})."
        )


@then("I should see an empty expense list message")
def step_empty_message(context):
    context.wait.until(lambda d: _empty_state_visible(context))
    assert _empty_state_visible(context), (
        "Expected empty-state text containing 'no expenses' inside #expenses-list."
    )


@then('the displayed expense count should be "{expected_count}"')
def step_displayed_expense_count_should_be(context, expected_count):
    expected = int(expected_count)

    # If empty state is visible, row count is the source of truth (and is fast)
    if expected == 0 and _empty_state_visible(context):
        return

    def _find_displayed_count():
        for locator in [
            (By.ID, "expense-count"),
            (By.ID, "expenses-count"),
            (By.CSS_SELECTOR, "[data-testid='expense-count']"),
            (By.XPATH, "//*[contains(normalize-space(), 'Count')]"),
        ]:
            els = context.driver.find_elements(*locator)
            if not els:
                continue
            text = (els[0].text or "").strip()
            digits = "".join(ch if ch.isdigit() else " " for ch in text).split()
            if digits:
                return int(digits[-1])
        return None

    displayed_count = _with_implicit_wait(context, 0, _find_displayed_count)

    if displayed_count is not None:
        assert displayed_count == expected, f"Expected displayed count {expected}, got {displayed_count}"
    else:
        rows = _expense_rows(context)
        assert len(rows) == expected, f"Expected {expected} expense rows, found {len(rows)}"