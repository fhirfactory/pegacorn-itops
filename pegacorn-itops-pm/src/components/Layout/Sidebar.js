import Tree from '@naisutech/react-tree';
import React from 'react';
import { MdDashboard } from 'react-icons/md';
import { Navbar } from 'reactstrap';
import bn from '../../utils/bemnames';

const navItems = [
  { to: '/', name: 'processing plants', exact: true, Icon: MdDashboard },
];

const bem = bn.create('sidebar');

export const AppContext = React.createContext([]);
class Sidebar extends React.Component {
  state = {
    isOpenComponents: true,
    isOpenContents: true,
    isOpenPages: true,
    data: [],
    menuData: [],
    refreshedData: []
  };

  static getDerivedStateFromProps(props, state) {
    console.log('received the new props: ' + JSON.stringify(props.data));
    return { refreshedData: props.data }
  }

  handleClick = name => () => {
    this.setState(prevState => {
      const isOpen = prevState[`isOpen${name}`];

      return {
        [`isOpen${name}`]: !isOpen,
      };
    });
  };

  selectMenuItem = (navigationMenuId) => {
    navigationMenuId = navigationMenuId[0];
    if (navigationMenuId) {
      this.props.menuClickHandler(navigationMenuId);
    }

    console.log("item infoss: " + navigationMenuId);
  }

  render() {
    return (
      <aside className={bem.b()}>
        <div className={bem.e('background')} />
        <div className={bem.e('content')}>
          <Navbar>
            <span className="text-white">
              ITOPS PM
            </span>
          </Navbar>

          <Tree nodes={this.state.refreshedData}
            isLoading={this.state.refreshedData.length < 1}
            animations={true}      
            onSelect={this.selectMenuItem} />
        </div>
      </aside>
    );
  }
}

export default Sidebar;
