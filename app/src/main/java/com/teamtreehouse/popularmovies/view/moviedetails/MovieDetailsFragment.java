package com.teamtreehouse.popularmovies.view.moviedetails;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.teamtreehouse.popularmovies.PopularMoviesApp;
import com.teamtreehouse.popularmovies.R;
import com.teamtreehouse.popularmovies.viewmodel.MovieDetailsViewModel;
import com.teamtreehouse.popularmovies.viewmodel.uimodels.MovieDetailsUiModel;
import com.teamtreehouse.popularmovies.viewmodel.uimodels.ReviewUiModel;
import com.teamtreehouse.popularmovies.viewmodel.uimodels.TrailerUiModel;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MovieDetailsFragment extends Fragment {
    private static final String TAG = "MovieDetailsFragment";

    private static final String ARG_MOVIE_ID = "com.teamtreehouse.popularmovies.ui.MovieDetialFragment.ARG_MOVIE_ID";

    public static Fragment newInstance(String movieId) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_MOVIE_ID, movieId);
        Fragment fragment = new MovieDetailsFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    private static final String SCROLL_VIEW_POSITION = "scroll_view_position";

    @Inject
    MovieDetailsViewModel mMovieDetailsViewModel;

    @BindView(R.id.movie_details_container)
    ScrollView mScrollViewContainer;
    @BindView(R.id.error_message)
    LinearLayout mErrorMessage;
    @BindView(R.id.retry_button)
    Button mRetryButton;
    @BindView(R.id.movie_poster_progressbar)
    ProgressBar mProgressBar;
    @BindView(R.id.movie_details)
    LinearLayout mMovieDetails;

    @BindView(R.id.movie_detail_title)
    TextView mTitle;
    @BindView(R.id.movie_detail_poster_thumbnail)
    ImageView mPosterThumbnail;
    @BindView(R.id.movie_detail_release_date)
    TextView mReleaseDate;
    @BindView(R.id.movie_detail_avg_rating)
    TextView mAvgRating;
    @BindView(R.id.favorites_button)
    ImageButton mFavoritesButton;
    @BindView(R.id.movie_detail_plot_synopsis)
    TextView mPlotSynopsis;
    @BindView(R.id.trailers)
    RecyclerView mTrailersView;
    @BindView(R.id.no_trailers_found)
    TextView mNoTrailersFound;
    @BindView(R.id.reviews)
    RecyclerView mReviewsView;
    @BindView(R.id.no_reviews_found)
    TextView mNoReviewsFound;
    private Unbinder mUnbinder;


    private MovieTrailersAdapter mTrailersAdapter;
    private MovieReviewsAdapter mReviewsAdapter;

    private Context mContext;

    private String mMovieId;
    private MovieDetailsUiModel mMovieDetailsUiModel;
    private boolean mIsFavorite;
    private int[] mScrollViewPosition;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMovieId = getArguments().getString(ARG_MOVIE_ID);
        mScrollViewPosition = new int[]{0, 0};
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_movie_detail, container, false);
        mContext = container.getContext();
        ((PopularMoviesApp) mContext.getApplicationContext())
                .getDataComponent()
                .inject(this);

        mUnbinder = ButterKnife.bind(this, view);

        showNoTrailersFound();
        setupTrailersRecyclerView();

        showNoReviewsFound();
        setupReviewsRecyclerView();
        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mScrollViewPosition = savedInstanceState.getIntArray(SCROLL_VIEW_POSITION);
        }

        Log.d(TAG, "onViewStateRestored: mScrollViewPosition " + mScrollViewPosition[0]);
    }

    private void setupTrailersRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        mTrailersAdapter = new MovieTrailersAdapter();
        mTrailersView.setLayoutManager(layoutManager);
        mTrailersView.setAdapter(mTrailersAdapter);
    }

    private void setupReviewsRecyclerView() {
        mReviewsAdapter = new MovieReviewsAdapter();
        mReviewsView.setLayoutManager(new LinearLayoutManager(mContext));
        mReviewsView.setAdapter(mReviewsAdapter);
    }


    @Override
    public void onResume() {
        super.onResume();

        if (mMovieId == null) {
            showErrorMessage();
            return;
        } else {
            showProgressBar();
            bind();
        }
    }

    private void bind() {

        mMovieDetailsViewModel.getMovieDetailsUiModel(mMovieId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<MovieDetailsUiModel>() {
                    @Override
                    public void onSuccess(MovieDetailsUiModel uiModel) {
                        mMovieDetailsUiModel = uiModel;
                        populateDetails();
                        loadTrailers(uiModel.getTrailers());
                        loadReviews(uiModel.getReviews());
                        setupFavoritesButton();
                        showMovieDetails();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "getMovieModel() - onError: ", e);
                    }
                });
    }

    private void loadTrailers(List<TrailerUiModel> trailers) {
        if (trailers.size() == 0) {
            showNoTrailersFound();
            return;
        }
        mTrailersAdapter.updateUI(trailers)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        mScrollViewContainer.scrollTo(mScrollViewPosition[0], mScrollViewPosition[1]);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: ", e);
                    }
                });
        showTrailers();
    }

    private void showNoTrailersFound() {
        mNoTrailersFound.setVisibility(View.VISIBLE);
        mTrailersView.setVisibility(View.GONE);
    }

    private void showTrailers() {
        mNoTrailersFound.setVisibility(View.GONE);
        mTrailersView.setVisibility(View.VISIBLE);
    }

    private void loadReviews(List<ReviewUiModel> reviews) {
        if (reviews.size() == 0) {
            showNoReviewsFound();
            return;
        }

        mReviewsAdapter.updateUI(reviews)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        mScrollViewContainer.scrollTo(mScrollViewPosition[0],mScrollViewPosition[1]);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: ", e);
                    }
                });

        showReviews();


    }

    private void showNoReviewsFound() {
        mNoReviewsFound.setVisibility(View.VISIBLE);
        mReviewsView.setVisibility(View.GONE);
    }

    private void showReviews() {
        mNoReviewsFound.setVisibility(View.GONE);
        mReviewsView.setVisibility(View.VISIBLE);
    }


    private void populateDetails() {
        mTitle.setText(mMovieDetailsUiModel.getOriginalTitle());
        Picasso.with(mContext)
                .load(mMovieDetailsUiModel.getImageThumbnailUrl())
                .error(R.drawable.movie_poster_error)
                .placeholder(R.drawable.movie_poster_placeholder)
                .into(mPosterThumbnail);
        mReleaseDate.setText(mMovieDetailsUiModel.getReleaseDate());
        mAvgRating.setText(String.valueOf(mMovieDetailsUiModel.getUserRating()));
        mPlotSynopsis.setText(mMovieDetailsUiModel.getPlotSynopsis());
    }

    private void setupFavoritesButton() {

        mMovieDetailsViewModel.isFavorite(mMovieId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean value) {
                        mIsFavorite = value;
                        updateFavoritesButton();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });

    }

    private void updateFavoritesButton() {

        if (!mIsFavorite) {

            mFavoritesButton.setBackgroundColor(getResources().getColor(R.color.notFavorite, null));
            mFavoritesButton.setOnClickListener(v -> {

                mMovieDetailsViewModel.addToFavorites()
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new DisposableCompletableObserver() {
                            @Override
                            public void onComplete() {
                                mIsFavorite = true;
                                updateFavoritesButton();
                                Toast.makeText(mContext, "Movie has been added to favorites", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "onError: addToFavorites()", e);
                                Toast.makeText(mContext, "ERROR: Not able to save to favorites", Toast.LENGTH_SHORT).show();
                            }
                        });
            });

        } else {

            mFavoritesButton.setBackgroundColor(getResources().getColor(R.color.isFavorite, null));
            mFavoritesButton.setOnClickListener(v -> {

                mMovieDetailsViewModel.removeFromFavorites(mMovieId)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new DisposableCompletableObserver() {
                            @Override
                            public void onComplete() {
                                mIsFavorite = false;
                                updateFavoritesButton();
                                Toast.makeText(mContext, "Movie has been removed to favorites", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "onError: removeFromFavorites()", e);
                                Toast.makeText(mContext, "ERROR: Not able to remove from favorites", Toast.LENGTH_SHORT).show();
                            }
                        });
            });

        }
    }

    private void showErrorMessage() {
        mRetryButton.setOnClickListener(v -> onResume());
        mErrorMessage.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        mMovieDetails.setVisibility(View.GONE);
    }

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mMovieDetails.setVisibility(View.GONE);
        mErrorMessage.setVisibility(View.GONE);

    }

    private void showMovieDetails() {
        mMovieDetails.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        mErrorMessage.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mScrollViewPosition[0] = mScrollViewContainer.getScrollX();
        mScrollViewPosition[1] = mScrollViewContainer.getScrollY();

        Log.d(TAG, "onPause: mScrollViewPosition " + mScrollViewPosition[1]);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState : mScrollViewPosition " + mScrollViewPosition[1]);
        outState.putIntArray(SCROLL_VIEW_POSITION, mScrollViewPosition);
    }

    @Override
    public void onDestroyView() {
        mUnbinder.unbind();
        super.onDestroyView();
    }
}
