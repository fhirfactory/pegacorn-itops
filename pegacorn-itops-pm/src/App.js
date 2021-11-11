import MainLayout from './components/Layout/MainLayout';
import PageSpinner from './components/PageSpinner';
import React from 'react';
import componentQueries from 'react-component-queries';
import './styles/app.scss';

class App extends React.Component {
  render() {
    return (
      <React.Suspense fallback={<PageSpinner />}>
        <MainLayout breakpoint={this.props.breakpoint}>
        </MainLayout>
      </React.Suspense>
    );
  }
};

const query = ({ width }) => {
  if (width < 575) {
    return { breakpoint: 'xs' };
  }

  if (576 < width && width < 767) {
    return { breakpoint: 'sm' };
  }

  if (768 < width && width < 991) {
    return { breakpoint: 'md' };
  }

  if (992 < width && width < 1199) {
    return { breakpoint: 'lg' };
  }

  if (width > 1200) {
    return { breakpoint: 'xl' };
  }

  return { breakpoint: 'xs' };
};

export default componentQueries(query)(App);
