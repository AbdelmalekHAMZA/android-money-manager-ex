/*
 * Copyright (C) 2012-2016 The Android Money Manager Ex Project Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.money.manager.ex.reports;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.money.manager.ex.R;
import com.money.manager.ex.common.MmexCursorLoader;
import com.money.manager.ex.database.SQLDataSet;
import com.money.manager.ex.database.ViewMobileData;
import com.money.manager.ex.common.BaseListFragment;
import com.money.manager.ex.utils.CalendarUtils;
import com.money.manager.ex.utils.MyDateTimeUtils;
import com.money.manager.ex.utils.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.Calendar;

public abstract class BaseReportFragment
    extends BaseListFragment
    implements LoaderCallbacks<Cursor> {

    protected static final int ID_LOADER = 1;
    protected static final String KEY_ITEM_SELECTED = "PayeeReportFragment:ItemSelected";
    protected static final String KEY_WHERE_CLAUSE = "PayeeReportFragment:WhereClause";
    protected static final String KEY_FROM_DATE = "PayeeReportFragment:FromDate";
    protected static final String KEY_TO_DATE = "PayeeReportFragment:ToDate";

    protected int mItemSelected = R.id.menu_all_time;
    protected String mWhereClause = null;
    protected DateTime mDateFrom = null;
    protected DateTime mDateTo = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //set list view
        setHasOptionsMenu(true);
        setEmptyText(getString(R.string.no_data));
        setListShown(false);

        // Restore instance state.
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_ITEM_SELECTED))
                mItemSelected = savedInstanceState.getInt(KEY_ITEM_SELECTED);
            if (savedInstanceState.containsKey(KEY_FROM_DATE)) {
                String dateFromString = savedInstanceState.getString(KEY_FROM_DATE);
                mDateFrom = DateTime.parse(dateFromString);
            }
            if (savedInstanceState.containsKey(KEY_TO_DATE))
                mDateTo = (DateTime) savedInstanceState.getSerializable(KEY_TO_DATE);
        }
        //start loader
        startLoader(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        //inflate menu
        inflater.inflate(R.menu.menu_report, menu);
        inflater.inflate(R.menu.menu_period_picker, menu);
        //checked item
        MenuItem item = menu.findItem(mItemSelected);
        if (item != null) {
            item.setChecked(true);
        }
    }

    // Loader events

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Loader<Cursor> result = null;

        switch (id) {
            case ID_LOADER:
                if (args != null && args.containsKey(KEY_WHERE_CLAUSE)) {
                    setWhereClause(args.getString(KEY_WHERE_CLAUSE));
                }
                String query = prepareQuery(getWhereClause());

                result = new MmexCursorLoader(getActivity(),  // context
                        new SQLDataSet().getUri(),          // uri
                        null,                               // projection
                        query,                              // selection
                        null,                               // selection args
                        null);                              // sort
                break;
        }
        return result;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case ID_LOADER:
                ((CursorAdapter) getListAdapter()).swapCursor(null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case ID_LOADER:
                ((CursorAdapter) getListAdapter()).swapCursor(data);
                if (isResumed()) {
                    setListShown(true);
                } else {
                    setListShownNoAnimation(true);
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_current_month:
                mDateFrom = MyDateTimeUtils.today().dayOfMonth().withMinimumValue();
                mDateTo = mDateFrom.dayOfMonth().withMaximumValue();
                break;

            case R.id.menu_last_month:
                mDateFrom = MyDateTimeUtils.today()
                        .minusMonths(1).dayOfMonth().withMinimumValue();
                mDateTo = mDateFrom.dayOfMonth().withMaximumValue();
                break;
            case R.id.menu_last_30_days:
                mDateFrom = MyDateTimeUtils.today().minusDays(30);
                mDateTo = MyDateTimeUtils.today();
                break;
            case R.id.menu_current_year:
                mDateFrom = MyDateTimeUtils.today()
                        .monthOfYear().withMinimumValue()
                        .dayOfMonth().withMinimumValue();
                mDateTo = mDateFrom.monthOfYear().withMaximumValue()
                        .dayOfMonth().withMaximumValue();
                break;
            case R.id.menu_last_year:
                mDateFrom = MyDateTimeUtils.today().minusYears(1)
                        .monthOfYear().withMinimumValue()
                        .dayOfMonth().withMinimumValue();
                mDateTo = mDateFrom
                        .monthOfYear().withMaximumValue()
                        .dayOfMonth().withMaximumValue();
                break;
            case R.id.menu_all_time:
                mDateFrom = null;
                mDateTo = null;
                break;
            case R.id.menu_custom_dates:
                //check item
                item.setChecked(true);
                mItemSelected = item.getItemId();
                //show dialog
                showDialogCustomDates();
                return true;
//                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        String whereClause = null;
        if (mDateFrom != null && mDateTo != null) {
            whereClause = ViewMobileData.Date + " >= '" + MyDateTimeUtils.getIsoStringFrom(mDateFrom) +
                "' AND " + ViewMobileData.Date + " <= '" + MyDateTimeUtils.getIsoStringFrom(mDateTo) + "'";
        }

        //check item
        item.setChecked(true);
        mItemSelected = item.getItemId();

        //compose bundle
        Bundle args = new Bundle();
        args.putString(KEY_WHERE_CLAUSE, whereClause);

        //starts loader
        startLoader(args);

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ITEM_SELECTED, mItemSelected);
        outState.putString(KEY_WHERE_CLAUSE, getWhereClause());
        if (mDateFrom != null) {
//            outState.putSerializable(KEY_FROM_DATE, mDateFrom);
            outState.putString(KEY_FROM_DATE, mDateFrom.toString());
        }
        if (mDateTo != null)
            outState.putSerializable(KEY_TO_DATE, mDateTo);
    }

    protected View addListViewHeaderFooter(int layout) {
        return View.inflate(getActivity(), layout, null);
    }

    /**
     * Prepare SQL query to execute in content provider
     *
     * @param whereClause
     * @return
     */
    protected abstract String prepareQuery(String whereClause);

    protected void setWhereClause(String mWhereClause) {
        this.mWhereClause = mWhereClause;
    }

    /**
     * Start loader with arguments
     *
     * @param args
     */
    protected void startLoader(Bundle args) {
        getLoaderManager().restartLoader(ID_LOADER, args, this);
    }

    protected String getWhereClause() {
        return mWhereClause;
    }

    private void showDialogCustomDates() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
            .customView(R.layout.dialog_choose_date_report, false)
            .positiveText(android.R.string.ok)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                    View view = materialDialog.getCustomView();
                    DatePicker fromDatePicker = (DatePicker) view.findViewById(R.id.datePickerFromDate);
                    DatePicker toDatePicker = (DatePicker) view.findViewById(R.id.datePickerToDate);

                    mDateFrom = MyDateTimeUtils.from(fromDatePicker);
                    mDateTo = MyDateTimeUtils.from(toDatePicker);

                    String whereClause =
                        ViewMobileData.Date + ">='" + MyDateTimeUtils.getIsoStringFrom(mDateFrom) + "' AND " +
                        ViewMobileData.Date + "<='" + MyDateTimeUtils.getIsoStringFrom(mDateTo) + "'";

                    Bundle args = new Bundle();
                    args.putString(KEY_WHERE_CLAUSE, whereClause);

                    startLoader(args);

                    //super.onPositive(dialog);
                }
            })
            .show();
        // set date if is null
        if (mDateFrom == null) mDateFrom = MyDateTimeUtils.today();
        if (mDateTo == null) mDateTo = MyDateTimeUtils.today();

        View view = dialog.getCustomView();
        DatePicker fromDatePicker = (DatePicker) view.findViewById(R.id.datePickerFromDate);
        DatePicker toDatePicker = (DatePicker) view.findViewById(R.id.datePickerToDate);

        MyDateTimeUtils.setDatePicker(mDateFrom, fromDatePicker);
        MyDateTimeUtils.setDatePicker(mDateTo, toDatePicker);
    }
}
