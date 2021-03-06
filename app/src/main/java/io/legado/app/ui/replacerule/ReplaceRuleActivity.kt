package io.legado.app.ui.replacerule

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.replacerule.edit.ReplaceEditDialog
import io.legado.app.utils.getViewModel
import io.legado.app.utils.splitNotBlank
import kotlinx.android.synthetic.main.activity_replace_rule.*
import kotlinx.android.synthetic.main.view_search.*


class ReplaceRuleActivity : VMBaseActivity<ReplaceRuleViewModel>(R.layout.activity_replace_rule),
    SearchView.OnQueryTextListener,
    ReplaceRuleAdapter.CallBack {
    override val viewModel: ReplaceRuleViewModel
        get() = getViewModel(ReplaceRuleViewModel::class.java)

    private lateinit var adapter: ReplaceRuleAdapter
    private var groups = hashSetOf<String>()
    private var groupMenu: SubMenu? = null
    private var replaceRuleLiveData: LiveData<List<ReplaceRule>>? = null
    private var dataInit = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        observeReplaceRuleData()
        observeGroupData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.replace_rule, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        groupMenu = menu?.findItem(R.id.menu_group)?.subMenu
        upGroupMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_replace_rule ->
                ReplaceEditDialog().show(supportFragmentManager, "replaceNew")
            R.id.menu_group_manage ->
                GroupManageDialog().show(supportFragmentManager, "groupManage")
            R.id.menu_select_all -> adapter.selectAll()
            R.id.menu_revert_selection -> adapter.revertSelection()
            R.id.menu_enable_selection -> viewModel.enableSelection(adapter.getSelectionIds())
            R.id.menu_disable_selection -> viewModel.disableSelection(adapter.getSelectionIds())
            R.id.menu_del_selection -> viewModel.delSelection(adapter.getSelectionIds())
            R.id.menu_export_selection -> viewModel.exportSelection(adapter.getSelectionIds())
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = ReplaceRuleAdapter(this, this)
        recycler_view.adapter = adapter
        recycler_view.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                ContextCompat.getDrawable(baseContext, R.drawable.ic_divider)?.let {
                    this.setDrawable(it)
                }
            })
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
    }

    private fun initSearchView() {
        ATH.setTint(search_view, primaryTextColor)
        search_view.onActionViewExpanded()
        search_view.queryHint = getString(R.string.replace_purify_search)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(this)
    }

    private fun observeReplaceRuleData(key: String? = null) {
        replaceRuleLiveData?.removeObservers(this)
        dataInit = false
        replaceRuleLiveData = if (key.isNullOrEmpty()) {
            App.db.replaceRuleDao().liveDataAll()
        } else {
            App.db.replaceRuleDao().liveDataSearch(key)
        }
        replaceRuleLiveData?.observe(this, Observer {
            if (dataInit) {
                setResult(Activity.RESULT_OK)
            }
            val diffResult = DiffUtil.calculateDiff(DiffCallBack(adapter.getItems(), it))
            adapter.setItems(it, false)
            diffResult.dispatchUpdatesTo(adapter)
            dataInit = true
        })
    }

    private fun observeGroupData() {
        App.db.replaceRuleDao().liveGroup().observe(this, Observer {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(",", ";"))
            }
            upGroupMenu()
        })
    }

    private fun upGroupMenu() {
        groupMenu?.removeGroup(R.id.source_group)
        groups.map {
            groupMenu?.add(R.id.source_group, Menu.NONE, Menu.NONE, it)
        }
    }


    override fun onQueryTextChange(newText: String?): Boolean {
        observeReplaceRuleData("%$newText%")
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun update(vararg rule: ReplaceRule) {
        viewModel.update(*rule)
    }

    override fun delete(rule: ReplaceRule) {
        viewModel.delete(rule)
    }

    override fun edit(rule: ReplaceRule) {
        ReplaceEditDialog
            .newInstance(rule.id)
            .show(supportFragmentManager, "editReplace")
    }

    override fun toTop(rule: ReplaceRule) {
        viewModel.toTop(rule)
    }

    override fun upOrder() {
        viewModel.upOrder()
    }
}