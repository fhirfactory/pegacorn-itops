import { Content, Footer, Header, Sidebar } from '../Layout';
import PageSpinner from '../PageSpinner';
import React from 'react';
import NotificationSystem from 'react-notification-system';
import { NOTIFICATION_SYSTEM_STYLE } from '../../utils/constants';
import DashboardPage from '../../pages/DashboardPage';

let daat = []

let treeDataNavMenu = [];

let processingPlants;
let workshops;
let wups;
let endpoints;

class MainLayout extends React.Component {
  state = {
    data: [],
    menuData: [],
    dashboardData: []
  };

  static isSidebarOpen() {
    return document
      .querySelector('.cr-sidebar')
      .classList.contains('cr-sidebar--open');
  }

  componentWillReceiveProps({ breakpoint }) {
    if (breakpoint !== this.props.breakpoint) {
      this.checkBreakpoint(breakpoint);
    }
  }

  static getDerivedStateFromProps(props, state) {
    return { menuData: state.menuData }
  }

  componentDidMount() {
    this.checkBreakpoint(this.props.breakpoint);

    fetch('http://localhost:18002/pegacorn/internal/itops/r1/ITOpsTopologyGraph')
      .then(res => res.json())
      .then((data) => {
        if (daat) {
          treeDataNavMenu = this.createNavigationMenu(data);
          this.setState(Object.assign({}, { data: data, menuData: treeDataNavMenu, dashboardData: data }));
        }
      });

    // NOTE: Below commented out code uses server sent push events, above uses fetch pull events. Need to put below back.

    // this.eventSource = new EventSource("http://localhost:8080/aether-hestia-audit-im-1.0.0-SNAPSHOT/rest/itops/subscribe");

    // this.eventSource.onmessage = e => {
    //   console.log("e.data onmessage: " + e.data);
    // };

    // this.eventSource.addEventListener("itops", (e) => {
    //   console.log("e.data onitops: " + e.data);
    // data = JSON.parse("[" + e.data + "]");

    //   if (data.length > 0) {  
    //     treeDataNavMenu = this.createNavigationMenu();
    //     this.setState(Object.assign({}, { data: data, menuData: treeDataNavMenu, dashboardData: data }));
    //   }
    // });

    // this.eventSource.onerror = e => {
    //   console.log("stop updates, crash...")
    //   this.stopUpdates();
    // }
  }

  createNavigationMenu = (data) => {
    const menus = [];
    Object.entries(data.processingPlants).forEach(([pKey, pValue]) => {
      menus.push({ label: pKey, id: pValue, parentId: null, items: null });
      Object.entries(pValue.workshops).forEach(([wKey, wValue]) => {
        menus.push({ label: wKey, id: wValue, parentId: pValue, items: null });
        Object.entries(wValue.workUnitProcessors).forEach(([wupKey, wupValue]) => {
          const items = [];
          Object.entries(wupValue.endpoints).forEach(([eKey, eValue]) => {
            items.push({ label: eKey, id: eValue, parentId: wupValue })
          });
          menus.push({ label: wupKey, id: wupValue, parentId: wValue, items: items });
        });
      });
    });
    return menus;
  }

  // close sidebar when
  handleContentClick = event => {
    // close sidebar if sidebar is open and screen size is less than `md`
    if (
      MainLayout.isSidebarOpen() &&
      (this.props.breakpoint === 'xs' ||
        this.props.breakpoint === 'sm' ||
        this.props.breakpoint === 'md')
    ) {
      this.openSidebar('close');
    }
  };

  handleMenuItemClick = navigationMenuId => {
    let selected = null;
    // if (processingPlants.has(navigationMenuId)) {
    //   selected = processingPlants.get(navigationMenuId);
    // } else if (workshops.has(navigationMenuId)) {
    //   selected = workshops.get(navigationMenuId);
    // } else if (wups.has(navigationMenuId)) {
    //   selected = wups.get(navigationMenuId);
    // } else {
    //   selected = endpoints.get(navigationMenuId);
    // }

    this.setState({ dashboardData: navigationMenuId });
  }

  checkBreakpoint(breakpoint) {
    switch (breakpoint) {
      case 'xs':
      case 'sm':
      case 'md':
        return this.openSidebar('close');

      case 'lg':
      case 'xl':
      default:
        return this.openSidebar('open');
    }
  }

  openSidebar(openOrClose) {
    if (openOrClose === 'open') {
      return document
        .querySelector('.cr-sidebar')
        .classList.add('cr-sidebar--open');
    }
    document.querySelector('.cr-sidebar').classList.remove('cr-sidebar--open');
  }

  render() {
    const { children } = this.props;
    return (
      <main className="cr-app bg-light">
        <React.Suspense fallback={<PageSpinner />}>
          <Sidebar data={this.state.menuData} menuClickHandler={this.handleMenuItemClick} />
          <Content fluid onClick={this.handleContentClick}>
            <Header />
            <DashboardPage data={this.state.dashboardData} />
            {children}
            <Footer />
          </Content>

          <NotificationSystem
            dismissible={false}
            ref={notificationSystem =>
              (this.notificationSystem = notificationSystem)
            }
            style={NOTIFICATION_SYSTEM_STYLE}
          />
        </React.Suspense>
      </main>
    );
  }
}

export default MainLayout;
