/**
 * 多级菜单组件
 * [To SouthWan]
 **/

Vue.component('light-menus', {
	props: {
		primary: {
			type: Number,
			default: undefined
		},
		menus: {
			type: Array,
			required: true
		},
		child: {
			type: Boolean,
			default: true
		}
	},
	data: function() {
		return {}
	},
	template: `<ul :class="[child ? 'nav-drawer' : 'nav nav-subnav']">
		<li
			v-for="(item, index) in menus"
			:class="
				'nav-item ' +
					(index === primary
						? 'active'
						: item.children !== undefined && item.children.length > 0
						? 'nav-item nav-item-has-subnav'
						: 'nav-item nav-item')
			"
		>
			<a 
				:class="[item.target === undefined ? (item.children !== undefined && item.children.length > 0 ? '' : 'multitabs') : '']"
				:href="item.href ?? 'javascript:void(0)'" :target="item.target ?? '_self'"
			>
				<i :class="'mdi ' + item.icon"></i>
				<span>{{ item.title }}</span>
			</a>
			<slot name="menu" :children="item.children ?? []" v-if="item.children !== undefined"></slot>
		</li>
	</ul>`
});
