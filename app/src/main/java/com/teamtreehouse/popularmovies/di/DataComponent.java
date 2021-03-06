package com.teamtreehouse.popularmovies.di;

import com.teamtreehouse.popularmovies.view.moviedetails.MovieDetailsFragment;
import com.teamtreehouse.popularmovies.view.movieposter.MoviePosterFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ApiModule.class,DataModelModule.class,ApplicationModule.class})
public interface DataComponent {
    void inject(MoviePosterFragment fragment);
    void inject(MovieDetailsFragment fragment);
}
