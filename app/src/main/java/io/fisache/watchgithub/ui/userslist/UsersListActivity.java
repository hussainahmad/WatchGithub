package io.fisache.watchgithub.ui.userslist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.jakewharton.rxbinding.widget.RxTextView;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fisache.watchgithub.R;
import io.fisache.watchgithub.base.AnalyticsManager;
import io.fisache.watchgithub.base.BaseActivity;
import io.fisache.watchgithub.base.BaseApplication;
import io.fisache.watchgithub.data.model.User;
import io.fisache.watchgithub.ui.repositorieslist.RepositoriesListActivity;
import io.fisache.watchgithub.ui.userdetail.UserDetailActivity;
import rx.Subscription;

public class UsersListActivity extends BaseActivity {

    @Bind(R.id.rvUserList)
    RecyclerView rvUserList;

    @Bind(R.id.pbLoading)
    ProgressBar pbLoading;

    @Bind(R.id.llUserExist)
    View llRepoExist;

    @Bind(R.id.llUserNotExist)
    View llRepoNotExist;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.btnAdd)
    FloatingActionButton btnAdd;

    @Bind(R.id.etSearch)
    EditText etSearch;

    @Inject
    UsersListActivityPresenter presenter;
    @Inject
    AnalyticsManager analyticsManager;
    @Inject
    UsersListAdapter usersListAdapter;
    @Inject
    LinearLayoutManager linearLayoutManager;
    @Inject
    AlertDialog.Builder addAlertDialog;

    private Subscription searchSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        ButterKnife.bind(this);

        //set up toolbar
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setTitle("Saved Github User");

        analyticsManager.logScreenView(getClass().getName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.subscribe();
        searchSubscription = presenter.observeSearchText(RxTextView.textChanges(etSearch));
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.unsubscribe();
        searchSubscription.unsubscribe();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_user_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itUserFilter :
                popUpUserMenuFilter();
                return true;
            default :
                break;
        }
        return true;
    }

    @Override
    public void setupActivityComponent() {
        BaseApplication.get(this)
                .getGroupComponent()
                .plus(new UsersListActivityModule(this))
                .inject(this);
    }

    public void showUsersListView() {
        rvUserList.setAdapter(usersListAdapter);
        rvUserList.setLayoutManager(linearLayoutManager);
    }

    public void showExistUsers() {
        llRepoExist.setVisibility(View.VISIBLE);
        llRepoNotExist.setVisibility(View.GONE);
        showUsersListView();
        showLoading(false);
    }

    public void showNotExistUsers() {
        llRepoExist.setVisibility(View.GONE);
        llRepoNotExist.setVisibility(View.VISIBLE);
    }

    public void showLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    public void setUsers(List<User> users) {
        usersListAdapter.updateUsersList(users);
    }

    public void onUserItemClicked(User user) {
        BaseApplication.get(this).createGithubUserComponent(user);
        RepositoriesListActivity.startWithUser(user, this);
    }

    public void onUserSettingClicked(User user) {
        UserDetailActivity.startWithUser(user, this);
    }

    @Override
    public void finish() {
        super.finish();
        BaseApplication.get(this).releaseGroupComponent();
    }

    @OnClick(R.id.btnAdd)
    public void onUserAddDialog() {
        addAlertDialog.setTitle("Add Github ID");
        addAlertDialog.setMessage("Be sure to enter");

        final EditText etLogin = new EditText(this);
        etLogin.setId(R.id.etGithubId);
        addAlertDialog.setView(etLogin);
        
        addAlertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                presenter.enterGithubUser(etLogin.getText().toString());
            }
        });

        addAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        addAlertDialog.show();
    }

    public void showVaildationError() {
        Snackbar.make(findViewById(android.R.id.content), "Write down exactly", Snackbar.LENGTH_LONG).show();
    }

    private void popUpUserMenuFilter() {
        PopupMenu popup = new PopupMenu(this, this.findViewById(R.id.itUserFilter));
        popup.getMenuInflater().inflate(R.menu.filter_user, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.filterUserAll:
                        presenter.setFilter(UserFilterType.ALL);
                        break;
                    case R.id.filterUserPopular:
                        presenter.setFilter(UserFilterType.POPULAR);
                        break;
                    case R.id.filterUserUser:
                        presenter.setFilter(UserFilterType.USER);
                        break;
                    case R.id.filterUserOrg:
                        presenter.setFilter(UserFilterType.ORG);
                        break;
                }
                presenter.setUsers();
                return true;
            }
        });
        popup.show();
    }
}
