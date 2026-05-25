import { useAuthStore } from './store/useAuthStore';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';

function App() {
  const token = useAuthStore((state) => state.token);

  if (!token) {
    return <Login />;
  }

  return <Dashboard />;
}

export default App;
