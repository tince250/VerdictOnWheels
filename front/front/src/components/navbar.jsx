import * as React from 'react';
import { Link } from 'react-router-dom';
// Remove this line - we'll use Link instead of navigate
// import { useNavigate } from 'react-router-dom';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import Menu from '@mui/material/Menu';
import MenuIcon from '@mui/icons-material/Menu';
import Container from '@mui/material/Container';
import Button from '@mui/material/Button';
import MenuItem from '@mui/material/MenuItem';
import AddIcon from '@mui/icons-material/Add';
import AdbIcon from '@mui/icons-material/Adb';
import verdicOnWheelsLogo from '../assets/verdicOnWheelsLogo.jpg';

// Navigation items with their route paths
const pages = [
  { name: 'Laws', path: '/laws' },
  { name: 'Judgments', path: '/judgments' }
];

function Navbar() {
  // Remove this line - we're not using navigate anymore
  // const navigate = useNavigate();
  const [anchorElNav, setAnchorElNav] = React.useState(null);

  const handleOpenNavMenu = (event) => {
    setAnchorElNav(event.currentTarget);
  };

  const handleCloseNavMenu = () => {
    setAnchorElNav(null);
  };

  return (
    <AppBar position="static" sx={{ width: '100%', bgcolor: '#FBF9F3', color: '#333333', paddingLeft: 3, paddingRight: 3 }}>
      <Container maxWidth="xl" disableGutters>
        <Toolbar disableGutters>
          {/* Use Link component instead of onClick navigate */}
          <Link to="/" style={{ textDecoration: 'none', color: 'inherit', display: 'flex', alignItems: 'center' }}>
            <img 
                src={verdicOnWheelsLogo} 
                alt="Verdict On Wheels Logo" 
                style={{ 
                height: 70, 
                marginRight: 8,

                display: { xs: 'none', md: 'flex' }
                }} 
            />
          </Link>
          
          {/* Use Link for logo text */}
          <Link to="/" style={{ textDecoration: 'none', color: 'inherit' }}>
            <Typography
              variant="h6"
              noWrap
              sx={{
                mr: 2,
                display: { xs: 'none', md: 'flex' },
                fontFamily: 'monospace',
                color: 'inherit',
              }}
            >
              Verdict On Wheels
            </Typography>
          </Link>

          {/* Mobile menu icon */}
          <Box sx={{ flexGrow: 1, display: { xs: 'flex', md: 'none' } }}>
            <IconButton
              size="large"
              aria-label="navigation menu"
              aria-controls="menu-appbar"
              aria-haspopup="true"
              onClick={handleOpenNavMenu}
              color="inherit"
            >
              <MenuIcon />
            </IconButton>
            <Menu
              id="menu-appbar"
              anchorEl={anchorElNav}
              anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'left',
              }}
              keepMounted
              transformOrigin={{
                vertical: 'top',
                horizontal: 'left',
              }}
              open={Boolean(anchorElNav)}
              onClose={() => handleCloseNavMenu()}
              sx={{ display: { xs: 'block', md: 'none' } }}
            >
              {/* Mobile menu items as Links */}
              {pages.map((page) => (
                <MenuItem 
                  key={page.name} 
                  onClick={handleCloseNavMenu}
                  component={Link}
                  to={page.path}
                >
                  <Typography sx={{ textAlign: 'center' }}>{page.name}</Typography>
                </MenuItem>
              ))}
              {/* Special Add New Case item in mobile menu */}
              <MenuItem 
                component={Link}
                to="/add-case"
                onClick={handleCloseNavMenu}
              >
                <Typography sx={{ 
                  textAlign: 'center', 
                  display: 'flex', 
                  alignItems: 'center'
                }}>
                  <AddIcon sx={{ mr: 0.5 }} />
                  Add New Case
                </Typography>
              </MenuItem>
            </Menu>
          </Box>

          {/* Mobile logo as Link */}
          <Link to="/" style={{ textDecoration: 'none', color: 'inherit', display: 'flex', alignItems: 'center' }}>
            <AdbIcon sx={{ display: { xs: 'flex', md: 'none' }, mr: 1, color: 'inherit' }} />
          </Link>
          
          <Link to="/" style={{ textDecoration: 'none', color: 'inherit', flexGrow: 1 }}>
            <Typography
              variant="h5"
              noWrap
              sx={{
                mr: 2,
                display: { xs: 'flex', md: 'none' },
                fontFamily: 'monospace',
                fontWeight: 700,
                letterSpacing: '.1rem',
                color: 'inherit',
              }}
            >
              Verdict on Wheels
            </Typography>
          </Link>

          {/* This empty box pushes the navigation buttons to the right */}
          <Box sx={{ flexGrow: 1, display: { md: 'block', xs: 'none' } }} />

          {/* Desktop navigation items */}
          <Box sx={{ display: { xs: 'none', md: 'flex' }, alignItems: 'center' }}>
            {pages.map((page) => (
              <Button
                key={page.name}
                component={Link}
                to={page.path}
                onClick={handleCloseNavMenu}
                sx={{ 
                  mx: 1, 
                  color: 'inherit',
                  '&:hover': {
                    backgroundColor: 'rgba(0,0,0,0.04)'
                  }
                }}
              >
                {page.name}
              </Button>
            ))}
            
            {/* Special Add New Case button */}
            <Button
              component={Link}
              to="/add-case"
              variant="contained"
              sx={{
                ml: 2,
                backgroundColor: '#EABA90',
                color: 'black',
                '&:hover': {
                  backgroundColor: '#D19A6A',
                },
                borderRadius: '10px',
                textTransform: 'none',
              }}
            >
              Add New Case
            </Button>
          </Box>
        </Toolbar>
      </Container>
    </AppBar>
  );
}
export default Navbar;