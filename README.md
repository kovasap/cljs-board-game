# Shifting World

Live version can be found at http://kovas.duckdns.org:3000/.  Note that this
is running on a raspeberry pi in my house so it may not be consistently up :).

## Game Flow

Every round, players place developments in order until they cannot place any
more and pass.

Then, the world shifts; the waning land type transforms into the next land type
in the cycle.
Optionally, a waxing land type transforms into the previous land type.

All developments on these shifted land tiles are destroyed, but resources stay.

The player with the highest score at the end wins!

## Feelings to Inspire in Players

 - Each new game should be exciting to start, as you look at the board and try
   to evaluate the best strategy for that specific board state.
 - Games should not be too long - players should not feel like they have to keep
   playing a game for a while that they have already lost.
 - I want to avoid the mid/late game civ problem of too many things going on.
   This game should feel like the early game of civ only.

## Strategic Journeys

Each round, players should be trying to solve the puzzle of how to connect
developments given that they require resources generated by other developments.
Additionally developments are constrained to certain tiles.

Over the course of the game, players should be thinking about how to accumulate
higher level resources so that they can be used in the next round to build even
better developments, or maybe the developments that are more productive that
round (based on round specific tiles).
For this to work, I need to add some constraint on players building a ton of
developments in a single round.
To start with I should just make this a fixed number of "days", with some
developments costing more days to build than others.

## Stuff to Add

In order of priority.

1. Make each development generate "public" resources that can only be consumed by other players, in addition to the resources you can consume.
1. Think about a "market" to act as a final scoring tile system like terra mystica.  Each round has a different "demand" for different resources - more can be accepted for more points.
1. Make it so that completing certain production chains allows you to build on
   some other tile types.
1. Finish affinity system that gives you more affinity the more developments you
   have on specific land types.
   E.g.
   a mountain would give you earth and fire affinity.
   Then make affinity relate to your score or ability to build other
   developments.
1. Make smaller development buttoms above the map that show the entire
   development card when you hover over them.
1. Add an incentive to build on or connect swaths of the same type of land.
   Perhaps your affinity is based on the size of the swaths you have at least one
   development in?
1. Implement the teraformer.
1. make developments more strictly tile specific.
1. Have left over resources stick around to the next round, and have them be
   useful in that round.
   Maybe some resources become "global" like points and can be used to build new
   developments anywhere.
   For instance a "blueprint" or "worker" resource that is required for every
   development.
1. Round specific tiles that control which way lands are waxing/waning.
1. Have a cult track where higher tiers in each land type allow you to terraform
   more terrain towards that terrain when it is waxing.
   You choose the terrain.
1. Add a deck of global effects (weather?) from which a set of cards is flipped
   each game.
   Each card has an effect that changes parameters across the whole game.
   This could include the old idea of "orders", which make some
   developments/resources more/less valuable in this game.
1. Possibly add a "protection" effect when a development is placed that prevents
   other players from using it's resources for a turn or two so that it's harder
   for people to steal your resources.
1. A tile selection system to use for all selection, use this for the
   terraformer tile selection.
1. https://github.com/timothypratley/reanimated?tab=readme-ov-file
1. Add better logging: https://github.com/ptaoussanis/sente/issues/416
1. Possibly host on glitch.com

## Setup

First, install dependencies:

    # Linuxbrew and clojure
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    brew install clojure/tools/clojure

    yarn

Then, start up all the servers with:

    ./run.zsh

You might need to open ports 3000, 5000, and 9630 to connect from another
machine.
On linux with UFW you can do this with:

```
sudo ufw allow 3000
sudo ufw allow 5000
sudo ufw allow 9630
```

### On Raspberry Pi

```
curl -sL https://deb.nodesource.com/setup_18.x | sudo bash -
sudo apt-get install nodejs
curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -
sudo apt install yarn
curl -O https://download.clojure.org/install/linux-install-1.11.1.1273.sh
chmod +x linux-install-1.11.1.1273.sh
sudo ./linux-install-1.11.1.1273.sh
sudo cp cljs-board-game.service /etc/systemd/system/
systemctl enable cljs-board-game.service
systemctl start cljs-board-game.service
# To read logs:
journalctl -u cljs-board-game.service
```

Set up port forwarding on router to forward ports 3000, 5000, 9630 to the
raspberry pi's IP address.

Set up duckdns to point to the IP at https://www.whatismyip.com/.

Now anyone can access the game at kovas.duckdns.org:3000!

See info about setting up a static domain name at
https://gist.github.com/taichikuji/6f4183c0af1f4a29e345b60910666468.

### Individual Server Startup

You can start the frontend service with:

    clj -M:frontend

You find the application at http://localhost:8700.

Start the backend API with this alias:

    clj -M:api

Find the backend server's documentation at: http://localhost:3000
