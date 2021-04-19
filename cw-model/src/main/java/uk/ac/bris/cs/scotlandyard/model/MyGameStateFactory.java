package uk.ac.bris.cs.scotlandyard.model;
import ch.qos.logback.classic.spi.EventArgUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 1: Complete this class
 */

public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull
	@Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		// 		TODO
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);

//		throw new RuntimeException("Implement me");
	}


	private static ImmutableSet<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		final var singleMoves = new ArrayList<SingleMove>();
		for (int destination : setup.graph.adjacentNodes(source)) {
			var occupied = false;
			// TODO: find out if destination is occupied by a detective
			for (final var p : detectives) {
				if (p.location() == destination) occupied = true;
			}
			if (occupied) continue;
			for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
				if (player.has(t.requiredTicket()))
					singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
			}
			if (player.has(Ticket.SECRET)) {
				singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
			}
			// TODO: add moves to the destination via a Secret ticket if there are any left with the player
		}
		return ImmutableSet.copyOf(singleMoves);
	}

	private static ImmutableSet<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		final var doubleMoves = new ArrayList<DoubleMove>();
		for (SingleMove first : makeSingleMoves(setup, detectives, player, source)) {
			ImmutableSet<SingleMove> second = makeSingleMoves(setup, detectives, player, first.destination);
			for (SingleMove s : second) {
				if ((first.ticket == s.ticket && player.hasAtLeast(first.ticket, 2)) || (first.ticket != s.ticket && player.has(first.ticket) && player.has(s.ticket))) {
					doubleMoves.add(new DoubleMove(player.piece(), source, first.ticket, first.destination, s.ticket, s.destination));
				}
			}
		}
		return ImmutableSet.copyOf(doubleMoves);
	}

	private static ImmutableSet<Move> Validmoves(ImmutableList<LogEntry> log, GameSetup setup, List<Player> detectives, Player player, int source) {
		final var validmoves = new ArrayList<Move>();
		validmoves.addAll(makeSingleMoves(setup, detectives, player, source));
		if ((player.has(Ticket.DOUBLE)) && log.size() <= setup.rounds.size() - 2) {
			validmoves.addAll(makeDoubleMoves(setup, detectives, player, source));
		}

		return ImmutableSet.copyOf(validmoves);
	}


	private final class MyGameState implements GameState {


		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Optional<Integer> getDetectiveLocation;
		private Optional<TicketBoard> getPlayerTickets;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;


		@Override
		public GameSetup getSetup() {
			return setup;
		};

		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> d = new LinkedHashSet<>();
			d.add(mrX.piece());
			for (final var p : detectives) {

				d.add(p.piece());
			}
			return ImmutableSet.copyOf(d);
		};

		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (final var p : detectives) {
				if (p.piece() == detective) return Optional.of(p.location());
			}
			return Optional.empty();
		};

		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			for (final var p : detectives) {
				if (p.piece() == piece) return Optional.of(ticket -> p.tickets().get(ticket));
			}
			if (mrX.piece() == piece) return Optional.of(ticket -> mrX.tickets().get(ticket));
			else return Optional.empty();


		};

		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		};

		@Override
		public ImmutableSet<Piece> getWinner() {

			Set<Piece> win = new LinkedHashSet<>();

			/*
			   1.if validmoves is empty that means tickets are empty, mrx loses
			   2. if in the final round, mrx wins
			*/
			Set<Move> mrxmove = new LinkedHashSet<>();

			mrxmove.addAll(Validmoves(log, setup, detectives, mrX, mrX.location()));

//			if (log.size() > setup.rounds.size()) {
//				return ImmutableSet.of();
//			}

			if (log.size() == setup.rounds.size() ) {
				win.add(mrX.piece());
				winner = ImmutableSet.copyOf(win);
			}



			  /*
            1. if all detectives singlemoves are empty that means all tickets are empty, detectives lose
            2. if all detectives location == mrx location , detectives win
			*/
			Set<Move> detect = new LinkedHashSet<>();

			for (final var p : detectives) {
				detect.addAll(makeSingleMoves(setup, detectives, p, p.location()));

					if (p.location() == mrX.location()) {
						for (final var detewin : detectives) {
						win.add(detewin.piece());
					}
					winner = ImmutableSet.copyOf(win);

				}

			}

			if (mrxmove.isEmpty()) {
				for (final var det : detectives) {
					win.add(det.piece());
				}
				winner = ImmutableSet.copyOf(win);
			}

			if (detect.isEmpty()){
				win.add(mrX.piece());
				winner = ImmutableSet.copyOf(win);
			}


			if (winner == null){
				return ImmutableSet.of();
			}

			return winner;

		};

		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> moves1 = new LinkedHashSet<>();

			if (winner != null && log.size()<setup.rounds.size()) {
				return ImmutableSet.of();
			}	else{
					for (final var b : remaining) {
						if (b == mrX.piece()) {
							if (log.size()<setup.rounds.size()){
								moves1.addAll(Validmoves(log, setup, detectives, mrX, mrX.location()));
							}
						} else {
							for (final var p : detectives) {

								if (b == p.piece()) moves1.addAll(makeSingleMoves(setup, detectives, p, p.location()));
							}
						}
					}

					return ImmutableSet.copyOf(moves1);
				}
		}


		@Override
		public GameState advance(Move move) {

			moves = getAvailableMoves();
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
//
//			List<LogEntry> logs = new ArrayList<>();

			var newDestination = move.visit(new Visitor<Integer>() {
				@Override
				public Integer visit(SingleMove singleMove) {
					return singleMove.destination;
				}

				@Override
				public Integer visit(DoubleMove doubleMove) {
					return doubleMove.destination2;
				}
			});

			if (move.commencedBy().isMrX()) {

				List<LogEntry> logs = new ArrayList<>();
				logs.addAll(log);

				move.visit(new Visitor<List<LogEntry>>() {

					@Override
					public List<LogEntry> visit(SingleMove singleMove) {
						if (setup.rounds.get(log.size())) {
							logs.add(LogEntry.reveal(singleMove.ticket, singleMove.destination));
						} else {
							logs.add(LogEntry.hidden(singleMove.ticket));
						}
//						mrX.use(singleMove.ticket);

						return logs;
					}

					@Override
					public List<LogEntry> visit(DoubleMove doubleMove) {

						if (setup.rounds.get(log.size())) {
							logs.add(LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1));

							if (setup.rounds.get(log.size() + 1)) {
								logs.add(LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2));
							} else {
								logs.add(LogEntry.hidden(doubleMove.ticket2));
							}
						} else {
							logs.add(LogEntry.hidden(doubleMove.ticket1));
							if (setup.rounds.get(log.size() + 1)) {
								logs.add(LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2));
							} else {
								logs.add(LogEntry.hidden(doubleMove.ticket2));
							}
						}
//						mrX.use(Ticket.DOUBLE);
//						mrX.use(doubleMove.ticket1);
//						mrX.use(doubleMove.ticket2);

						return logs;
					}
				});


				Set<Piece> Remain = new LinkedHashSet<>();
				for (final var detect : detectives) {
					Remain.add(detect.piece());
				}
//
//				mrX.use(move.tickets());
//				mrX.at(newDestination);
//
//				mrX = mrX.use(move.tickets()).at(newDestination);

				return new MyGameState(setup, ImmutableSet.copyOf(Remain), ImmutableList.copyOf(logs), mrX.use(move.tickets()).at(newDestination), detectives);


			} else {

				final var newDetectives = new ArrayList<Player>();
				Set<Piece> newRemaining = new LinkedHashSet<>(remaining);
				Set<Piece> newRemain = new LinkedHashSet<>();



				for (final var d : detectives) {
					if (d.piece() == move.commencedBy()) {
						newRemaining.remove(d.piece());
//						d.use(move.tickets());
//						d.at(newDestination);
						newDetectives.add(d.use(move.tickets()).at(newDestination));

					}
					else if (makeSingleMoves(setup,detectives,d,d.location()).isEmpty()){
						newRemaining.remove(d.piece());
						newDetectives.add(d);
					}
					else{
						newDetectives.add(d);
					}

				}

				if (newRemaining.isEmpty()) {
					newRemain.add(mrX.piece());
				} else {
					newRemain = newRemaining;
				}


				return new MyGameState(setup, ImmutableSet.copyOf(newRemain), log, mrX.give(move.tickets()), newDetectives);

			}
		}



		private MyGameState( final GameSetup setup,
			final ImmutableSet<Piece> remaining,
			final ImmutableList<LogEntry> log,
			final Player mrX,
			final List<Player> detectives
		){
				this.setup = setup;
				this.remaining = remaining;
				this.log = log;
				this.mrX = mrX;
				this.detectives = detectives;

				if (setup.rounds.isEmpty() || setup.graph.nodes().isEmpty()) throw new IllegalArgumentException();

				if (mrX.piece() == null || detectives.contains(null)) throw new NullPointerException();

				for (final var p : detectives) {
					if (p.has(Ticket.DOUBLE) || p.has(Ticket.SECRET)) throw new IllegalArgumentException();
				}
				for (int a = 0; a < detectives.size(); a++) {
					for (int b = a + 1; b < detectives.size(); b++) {
						if (detectives.get(a).location() == detectives.get(b).location())
							throw new IllegalArgumentException();
					}
				}

			}

	}
}