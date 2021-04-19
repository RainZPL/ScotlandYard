package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import uk.ac.bris.cs.scotlandyard.model.Model.Observer;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;
import uk.ac.bris.cs.scotlandyard.model.MyGameStateFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private MyGameStateFactory GSfactory = new MyGameStateFactory();


	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		// TODO
		return new mymodel(GSfactory.build(setup, mrX, detectives));


	}


	private final class mymodel implements Model{

		private  GameState gameState;
		Set<Observer> observers = new HashSet<Observer>();


		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return gameState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			requireNonNull(observer);
			if (!observers.contains(observer)) {
				observers.add(observer);
			} else throw new IllegalArgumentException();
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			requireNonNull(observer);
			if (observers.contains(observer)) {
				observers.remove(observer);
			} else throw new IllegalArgumentException();
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
//			return null;
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			gameState = gameState.advance(move);
			var event = gameState.getWinner().isEmpty() ? Observer.Event.MOVE_MADE : Observer.Event.GAME_OVER;
			for (Observer o : observers) o.onModelChanged(gameState, event);
		}


//		constructor
		private  mymodel(final GameState gameState){
			this.gameState = gameState;
		}
	}
}
