import Badge from "@mui/material/Badge";
import IconButton from "@mui/material/IconButton";
import Popover from "@mui/material/Popover";
import PersonIcon from "@mui/icons-material/Person";
import React from "react";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";

export default function UserIcon({
  username,
  logout,
}: {
  username: string;
  logout: () => void;
}) {
  const [anchorEl, setAnchorEl] = React.useState<HTMLButtonElement | null>(
    null
  );

  const openPopover = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const closePopover = () => {
    setAnchorEl(null);
  };

  const open = Boolean(anchorEl);

  return (
    <React.Fragment>
      <IconButton color="inherit" onClick={openPopover}>
        <Badge color="secondary">
          <PersonIcon />
        </Badge>
      </IconButton>
      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={closePopover}
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "center",
        }}
        transformOrigin={{
          vertical: "top",
          horizontal: "center",
        }}
        style={{ textAlign: "center" }}
      >
        <Typography sx={{ p: 2 }}>{username}</Typography>
        <Button variant="text" onClick={logout}>
          Logout
        </Button>
      </Popover>
    </React.Fragment>
  );
}
